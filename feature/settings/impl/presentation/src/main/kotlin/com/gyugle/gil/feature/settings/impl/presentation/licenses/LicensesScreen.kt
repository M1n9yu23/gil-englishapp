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
package com.gyugle.gil.feature.settings.impl.presentation.licenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.RowDivider
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.settings.impl.presentation.R
import com.gyugle.gil.feature.settings.impl.presentation.component.LinkRow

@Composable
fun LicensesRoot(onBack: () -> Unit, onOpenLibrary: (String) -> Unit, viewModel: LicensesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LicensesScreen(state = state, onBack = onBack, onOpenLibrary = onOpenLibrary, onRetry = viewModel::onRetryClick)
}

@Composable
fun LicensesScreen(
    state: LicensesState,
    onBack: () -> Unit,
    onOpenLibrary: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(R.string.settings_licenses), onNavigateBack = onBack)
        when (state) {
            LicensesState.Loading -> LoadingState()
            LicensesState.Failed -> LoadFailed(onRetry)
            is LicensesState.Loaded -> LibraryList(state, onOpenLibrary)
        }
    }
}

@Composable
private fun LibraryList(state: LicensesState.Loaded, onOpenLibrary: (String) -> Unit) {
    val spacing = GilTheme.spacing
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = spacing.inlineGap,
            bottom = spacing.sectionGap + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        )
    ) {
        item(key = "intro") {
            Text(
                text = stringResource(R.string.settings_licenses_body),
                modifier = Modifier.padding(horizontal = spacing.screenMargin, vertical = spacing.inlineGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        itemsIndexed(state.libraries, key = { _, library -> library.id }) { index, library ->
            if (index > 0) RowDivider()
            LinkRow(
                headline = library.name,
                supporting = library.licenses.joinToString { it.name }.ifBlank { null },
                onClick = { onOpenLibrary(library.id) }
            )
        }
    }
}

@Composable
fun LicenseRoot(libraryId: String, onBack: () -> Unit, viewModel: LicensesViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LicenseScreen(state = state, libraryId = libraryId, onBack = onBack, onRetry = viewModel::onRetryClick)
}

/** One library's full license text, selectable so it can be copied. */
@Composable
fun LicenseScreen(
    state: LicensesState,
    libraryId: String,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val library = (state as? LicensesState.Loaded)?.libraries?.firstOrNull { it.id == libraryId }
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = library?.name.orEmpty(), onNavigateBack = onBack)
        when {
            state == LicensesState.Loading -> LoadingState()
            library == null -> LoadFailed(onRetry)
            else -> LicenseText(library)
        }
    }
}

@Composable
private fun LicenseText(library: LibraryUi) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = spacing.screenMargin)
            .padding(top = spacing.inlineGap, bottom = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        val details = listOfNotNull(
            library.version?.let { stringResource(R.string.settings_license_version, it) },
            library.developers
        )
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString("\n"),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
        library.licenses.forEach { license ->
            Text(
                text = license.name,
                modifier = Modifier
                    .padding(top = spacing.itemGap)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )
            license.text?.let { text ->
                SelectionContainer {
                    Text(text = text, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
                }
            }
        }
    }
}

@Composable
private fun LoadFailed(onRetry: () -> Unit) {
    ErrorState(
        title = stringResource(R.string.settings_licenses_error_title),
        body = stringResource(R.string.settings_licenses_error_body),
        retryLabel = stringResource(R.string.settings_licenses_retry),
        onRetry = onRetry
    )
}

internal val PreviewLibraries = listOf(
    LibraryUi(
        id = "com.github.orioncactus:pretendard",
        name = "Pretendard",
        version = "1.3.9",
        developers = "Kil Hyung-jin",
        licenses = listOf(LicenseUi("SIL Open Font License 1.1", "Copyright (c) 2021, Kil Hyung-jin"))
    ),
    LibraryUi(
        id = "androidx.room:room-runtime",
        name = "Room",
        version = "2.8.4",
        developers = "The Android Open Source Project",
        licenses = listOf(LicenseUi("Apache License 2.0", "Apache License\nVersion 2.0, January 2004"))
    )
)

@PreviewLightDark
@Composable
private fun LicensesScreenPreview() {
    GilTheme {
        Surface {
            LicensesScreen(LicensesState.Loaded(PreviewLibraries), onBack = {}, onOpenLibrary = {}, onRetry = {})
        }
    }
}
