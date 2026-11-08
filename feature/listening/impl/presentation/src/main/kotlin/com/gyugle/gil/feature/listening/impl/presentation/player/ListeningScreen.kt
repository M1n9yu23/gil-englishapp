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
package com.gyugle.gil.feature.listening.impl.presentation.player

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.ChoiceDialog
import com.gyugle.gil.core.designsystem.component.ChoiceRow
import com.gyugle.gil.core.designsystem.component.EmptyState
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilProgressBar
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.RowDivider
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.SectionTitle
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.PauseLength
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.presentation.R

private object ListeningTokens {
    /** The play button stands out from the two skip buttons beside it. */
    val PlayButtonSize = 72.dp
    val PlayIconSize = 36.dp
    val SkipButtonSize = 56.dp
}

@Composable
fun ListeningRoot(onClose: () -> Unit, viewModel: ListeningViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ListeningEvent.OpenVoiceInstall -> try {
                context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            } catch (_: ActivityNotFoundException) {
                viewModel.onAction(ListeningAction.OnInstallUnavailable)
            }

            is ListeningEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.resolve(context))
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(ListeningAction.OnResumed) }
    ListeningScreen(
        state = state,
        onAction = viewModel::onAction,
        onClose = onClose,
        snackbarHostState = snackbarHostState
    )
}

/**
 * Listening mode: pick what to hear, wait while the audio is made, then a player the lock screen
 * and headphones also control. Closing the screen keeps it playing.
 */
@Composable
fun ListeningScreen(
    state: ListeningState,
    onAction: (ListeningAction) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GilTopBar(title = stringResource(R.string.listening_title), onNavigateBack = onClose)
            when (state) {
                ListeningState.Loading -> LoadingState()

                ListeningState.NoEngine -> EmptyState(
                    title = stringResource(R.string.listening_no_engine_title),
                    body = stringResource(R.string.listening_no_engine_body)
                )

                ListeningState.NoEnglishVoice -> EmptyState(
                    title = stringResource(R.string.listening_no_voice_title),
                    body = stringResource(R.string.listening_no_voice_body),
                    actionLabel = stringResource(R.string.listening_install),
                    onAction = { onAction(ListeningAction.OnInstallClick) }
                )

                ListeningState.Failed -> ErrorState(
                    title = stringResource(R.string.listening_error_title),
                    body = stringResource(R.string.listening_error_body),
                    retryLabel = stringResource(R.string.listening_error_retry),
                    onRetry = { onAction(ListeningAction.OnRetryClick) }
                )

                ListeningState.NothingYet -> EmptyState(
                    title = stringResource(R.string.listening_empty_title),
                    body = stringResource(R.string.listening_empty_body)
                )

                is ListeningState.Choosing -> ChoosingContent(state, onAction)

                is ListeningState.Preparing -> PreparingContent(state, onAction)

                is ListeningState.Playing -> PlayingContent(state, onAction)

                is ListeningState.Ended -> EndedContent(state, onAction)
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

@Composable
private fun ChoosingContent(state: ListeningState.Choosing, onAction: (ListeningAction) -> Unit) {
    val spacing = GilTheme.spacing
    val margin = Modifier.padding(horizontal = spacing.screenMargin)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(top = spacing.itemGap, bottom = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        Column(modifier = margin, verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            KoreanProse(
                text = stringResource(R.string.listening_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!state.koreanVoice) {
                KoreanProse(
                    text = stringResource(R.string.listening_no_korean),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SecondaryActionButton(
                    text = stringResource(R.string.listening_install),
                    onClick = { onAction(ListeningAction.OnInstallClick) }
                )
            }
        }
        Section(stringResource(R.string.listening_section_sources)) {
            val rows = listOfNotNull(state.resume?.let { it to true }) + state.sources.map { it to false }
            rows.forEachIndexed { index, (source, resume) ->
                if (index > 0) RowDivider()
                SourceRow(source, resume, onAction)
            }
        }
        if (state.units.isNotEmpty()) {
            Section(stringResource(R.string.listening_section_units)) {
                state.units.forEachIndexed { index, unit ->
                    if (index > 0) RowDivider()
                    SourceRow(unit, resume = false, onAction = onAction)
                }
            }
        }
        Section(stringResource(R.string.listening_section_pace)) {
            ListRow(
                headline = stringResource(R.string.listening_speed),
                supporting = state.pace.speed.label(),
                onClick = { onAction(ListeningAction.OnSpeedClick) }
            )
            RowDivider()
            ListRow(
                headline = stringResource(R.string.listening_pause),
                supporting = state.pace.pause.label(),
                onClick = { onAction(ListeningAction.OnPauseLengthClick) }
            )
        }
    }
    val dismiss = { onAction(ListeningAction.OnDialogDismiss) }
    when (state.dialog) {
        PaceDialog.SPEED -> ChoiceDialog(
            title = stringResource(R.string.listening_speed),
            dismissLabel = stringResource(R.string.listening_dialog_close),
            onDismiss = dismiss
        ) {
            ListeningSpeed.entries.forEach { speed ->
                DialogChoice(speed.label(), speed == state.pace.speed) { onAction(ListeningAction.OnSpeedPick(speed)) }
            }
        }

        PaceDialog.PAUSE -> ChoiceDialog(
            title = stringResource(R.string.listening_pause),
            dismissLabel = stringResource(R.string.listening_dialog_close),
            onDismiss = dismiss
        ) {
            PauseLength.entries.forEach { pause ->
                DialogChoice(pause.label(), pause == state.pace.pause) {
                    onAction(ListeningAction.OnPauseLengthPick(pause))
                }
            }
        }

        null -> Unit
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SectionTitle(title)
        content()
    }
}

/** A source to play. The resume row names the source it goes back to. */
@Composable
private fun SourceRow(source: SourceUi, resume: Boolean, onAction: (ListeningAction) -> Unit) {
    val count = if (source.source == PlaylistSource.Dialogues) {
        pluralStringResource(R.plurals.listening_dialogue_count, source.count, source.count)
    } else {
        pluralStringResource(R.plurals.listening_item_count, source.count, source.count)
    }
    ListRow(
        headline = if (resume) stringResource(R.string.listening_resume) else source.name.asString(),
        supporting = if (resume) source.name.asString() else count,
        onClick = {
            onAction(if (resume) ListeningAction.OnResumeClick else ListeningAction.OnSourceClick(source.source))
        },
        trailing = {
            Icon(
                painter = GilIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
private fun DialogChoice(label: String, selected: Boolean, onSelect: () -> Unit) {
    ChoiceRow(label = label, selected = selected, onSelect = onSelect, horizontalPadding = 0.dp)
}

@Composable
private fun PreparingContent(state: ListeningState.Preparing, onAction: (ListeningAction) -> Unit) {
    val spacing = GilTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = spacing.screenMargin)
            .padding(top = spacing.itemGap, bottom = spacing.screenMargin)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            Text(text = stringResource(R.string.listening_preparing_title), style = MaterialTheme.typography.titleLarge)
            KoreanProse(
                text = stringResource(R.string.listening_preparing_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.size(spacing.inlineGap))
            GilProgressBar(
                progress = if (state.total == 0) 0f else state.done.toFloat() / state.total,
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
            )
            if (state.total > 0) {
                Text(
                    text = stringResource(R.string.listening_preparing_progress, state.done, state.total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        SecondaryActionButton(
            text = stringResource(R.string.listening_preparing_cancel),
            onClick = { onAction(ListeningAction.OnCancelPreparingClick) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlayingContent(state: ListeningState.Playing, onAction: (ListeningAction) -> Unit) {
    val spacing = GilTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = spacing.screenMargin)
            .padding(top = spacing.itemGap, bottom = spacing.screenMargin)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            Text(
                text = stringResource(R.string.listening_position, state.position, state.count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // The text above says the same.
            GilProgressBar(
                progress = state.position.toFloat() / state.count,
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics {}
            )
            Spacer(Modifier.size(spacing.itemGap))
            // Read out as each track starts, for a learner listening with TalkBack.
            Column(
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)
            ) {
                EnglishText(text = state.track.title, size = EnglishTextSize.Focus)
                state.track.subtitle?.let {
                    KoreanProse(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!state.playing) {
                Text(
                    text = stringResource(R.string.listening_paused),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        PlayerControls(state.playing, onAction)
        Spacer(Modifier.size(spacing.sectionGap))
        SecondaryActionButton(
            text = stringResource(R.string.listening_stop),
            onClick = { onAction(ListeningAction.OnStopClick) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PlayerControls(playing: Boolean, onAction: (ListeningAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.sectionGap, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onAction(ListeningAction.OnPreviousClick) },
            modifier = Modifier.size(ListeningTokens.SkipButtonSize)
        ) {
            Icon(
                painter = GilIcons.SkipPreviousFilled,
                contentDescription = stringResource(R.string.listening_previous),
                tint = colors.onSurface
            )
        }
        FilledIconButton(
            onClick = { onAction(ListeningAction.OnPlayPauseClick) },
            modifier = Modifier.size(ListeningTokens.PlayButtonSize)
        ) {
            Icon(
                painter = if (playing) GilIcons.PauseFilled else GilIcons.PlayFilled,
                contentDescription = stringResource(
                    if (playing) R.string.listening_pause_playback else R.string.listening_play
                ),
                modifier = Modifier.size(ListeningTokens.PlayIconSize)
            )
        }
        IconButton(
            onClick = { onAction(ListeningAction.OnNextClick) },
            modifier = Modifier.size(ListeningTokens.SkipButtonSize)
        ) {
            Icon(
                painter = GilIcons.SkipNextFilled,
                contentDescription = stringResource(R.string.listening_next),
                tint = colors.onSurface
            )
        }
    }
}

@Composable
private fun EndedContent(state: ListeningState.Ended, onAction: (ListeningAction) -> Unit) {
    val spacing = GilTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(horizontal = spacing.screenMargin)
            .padding(top = spacing.itemGap, bottom = spacing.screenMargin)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            Text(text = stringResource(R.string.listening_ended_title), style = MaterialTheme.typography.titleLarge)
            KoreanProse(
                text = pluralStringResource(R.plurals.listening_ended_body, state.count, state.count),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(spacing.itemGap)) {
            PrimaryActionButton(
                text = stringResource(R.string.listening_play_again),
                onClick = { onAction(ListeningAction.OnPlayAgainClick) },
                modifier = Modifier.fillMaxWidth()
            )
            SecondaryActionButton(
                text = stringResource(R.string.listening_choose_another),
                onClick = { onAction(ListeningAction.OnStopClick) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ListeningSpeed.label(): String = stringResource(
    when (this) {
        ListeningSpeed.NORMAL -> R.string.listening_speed_normal
        ListeningSpeed.SLOW -> R.string.listening_speed_slow
    }
)

@Composable
private fun PauseLength.label(): String {
    val seconds = duration.inWholeSeconds.toInt()
    return pluralStringResource(R.plurals.listening_pause_seconds, seconds, seconds)
}

@PreviewLightDark
@Composable
private fun ListeningChoosingPreview() {
    GilTheme { Surface { ListeningScreen(state = ListeningPreviewData.choosing, onAction = {}, onClose = {}) } }
}

@PreviewLightDark
@Composable
private fun ListeningPlayingPreview() {
    GilTheme { Surface { ListeningScreen(state = ListeningPreviewData.playing, onAction = {}, onClose = {}) } }
}
