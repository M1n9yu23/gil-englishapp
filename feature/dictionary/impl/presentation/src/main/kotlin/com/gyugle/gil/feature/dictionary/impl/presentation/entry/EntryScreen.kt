/*
 * Copyright 2026 MinGyu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gyugle.gil.feature.dictionary.impl.presentation.entry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.core.presentation.rememberContentReporter
import com.gyugle.gil.feature.dictionary.impl.presentation.ContentErrorUi
import com.gyugle.gil.feature.dictionary.impl.presentation.R
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ContentErrorView
import com.gyugle.gil.feature.dictionary.impl.presentation.component.EntryLoading

@Composable
internal fun EntryRoot(
    entryId: String,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onOpenSentence: (sentenceId: Long, entryId: String) -> Unit,
    viewModel: EntryViewModel = hiltViewModel<EntryViewModel, EntryViewModel.Factory>(
        creationCallback = { factory -> factory.create(entryId) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is EntryEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.resolve(context))
        }
    }
    val reporter = rememberContentReporter()
    val navigation = remember(onBack, onOpenEntry, onOpenSentence) {
        EntryNavigation(onBack = onBack, onOpenEntry = onOpenEntry, onOpenSentence = onOpenSentence)
    }
    EntryScreen(
        state = state,
        onAction = viewModel::onAction,
        navigation = navigation,
        onReport = if (reporter.canReport) ({ entry -> reporter.report(entry.id, entry.headline) }) else null,
        snackbarHostState = snackbarHostState
    )
}

/** [onReport] is null when no app can open the report form. */
@Composable
internal fun EntryScreen(
    state: EntryState,
    onAction: (EntryAction) -> Unit,
    navigation: EntryNavigation,
    onReport: ((EntryUi) -> Unit)?,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            val entry = state.entry
            GilTopBar(
                // Empty while loading; the bar keeps its height so the page does not jump.
                title = entry?.let { stringResource(it.kindTitle()) }.orEmpty(),
                onNavigateBack = navigation.onBack,
                actions = { if (entry != null && onReport != null) ReportMenu(onReport = { onReport(entry) }) }
            )
            // Keyed by kind of content, so saving a word updates the page in place and keeps its scroll.
            val fade = GilTheme.motion.fade
            AnimatedContent(
                targetState = state.content(),
                transitionSpec = {
                    fadeIn(tween(fade)) togetherWith fadeOut(tween(fade))
                },
                contentKey = { it::class },
                label = "entry"
            ) {
                when (it) {
                    EntryContent.Loading -> EntryLoading()

                    is EntryContent.Failed -> ContentErrorView(
                        error = it.error,
                        onRetry = { onAction(EntryAction.OnRetryClick) },
                        onBack = navigation.onBack
                    )

                    is EntryContent.Loaded -> EntryPage(it.entry, state.voices, onAction, navigation)
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        )
    }
}

private sealed interface EntryContent {
    data object Loading : EntryContent
    data class Failed(val error: ContentErrorUi) : EntryContent
    data class Loaded(val entry: EntryUi) : EntryContent
}

private fun EntryState.content(): EntryContent = when {
    error != null -> EntryContent.Failed(error)
    isLoading || entry == null -> EntryContent.Loading
    else -> EntryContent.Loaded(entry)
}

private fun EntryUi.kindTitle(): Int = when (this) {
    is EntryUi.Word -> R.string.dictionary_entry_word
    is EntryUi.Pattern -> R.string.dictionary_entry_pattern
    is EntryUi.Sound -> R.string.dictionary_entry_sound
}

@Composable
private fun ReportMenu(onReport: () -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(painter = GilIcons.MoreVert, contentDescription = stringResource(R.string.dictionary_more))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.dictionary_report)) },
                onClick = {
                    open = false
                    onReport()
                }
            )
        }
    }
}

@Composable
private fun EntryPage(entry: EntryUi, voices: VoicesUi, onAction: (EntryAction) -> Unit, navigation: EntryNavigation) {
    val spacing = GilTheme.spacing
    // A pushed screen has no bottom bar, so it keeps its end clear of the navigation bar.
    val navigationBar = WindowInsets.navigationBars.asPaddingValues()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = spacing.screenMargin,
                end = spacing.screenMargin,
                bottom = spacing.sectionGap + navigationBar.calculateBottomPadding()
            )
    ) {
        when (entry) {
            is EntryUi.Word -> WordPage(entry, voices, onAction, navigation)
            is EntryUi.Pattern -> PatternPage(entry, voices, onAction, navigation)
            is EntryUi.Sound -> SoundPage(entry, voices, onAction)
        }
    }
}

@PreviewLightDark
@Composable
private fun EntryScreenPreview() {
    GilTheme {
        Surface {
            EntryScreen(
                state = EntryState(isLoading = false, entry = EntryPreviewData.word, voices = VoicesUi(true, true)),
                onAction = {},
                navigation = EntryNavigation({}, {}, { _, _ -> }),
                onReport = {},
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
