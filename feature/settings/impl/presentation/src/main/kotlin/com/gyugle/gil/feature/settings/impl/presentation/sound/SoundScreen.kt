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
package com.gyugle.gil.feature.settings.impl.presentation.sound

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.impl.presentation.R
import com.gyugle.gil.feature.settings.impl.presentation.component.ChoiceRow
import com.gyugle.gil.feature.settings.impl.presentation.component.RowDivider
import com.gyugle.gil.feature.settings.impl.presentation.component.SettingsSection
import com.gyugle.gil.feature.settings.impl.presentation.component.label

@Composable
fun SoundRoot(onBack: () -> Unit, viewModel: SoundViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SoundEvent.OpenVoiceInstall -> try {
                context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            } catch (_: ActivityNotFoundException) {
                viewModel.onAction(SoundAction.OnInstallUnavailable)
            }

            is SoundEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.resolve(context))
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(SoundAction.OnResumed) }
    SoundScreen(state = state, onAction = viewModel::onAction, onBack = onBack, snackbarHostState = snackbarHostState)
}

@Composable
fun SoundScreen(
    state: SoundState,
    onAction: (SoundAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            GilTopBar(title = stringResource(R.string.settings_sound), onNavigateBack = onBack)
            val accent = state.accent
            if (accent == null) LoadingState() else SoundContent(state, accent, onAction)
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
private fun SoundContent(state: SoundState, accent: Accent, onAction: (SoundAction) -> Unit) {
    val spacing = GilTheme.spacing
    val margin = Modifier.padding(horizontal = spacing.screenMargin)
    val british = (state.voices as? VoicesUi.Known)?.british
    val hasBritish = british == VoiceState.INSTALLED
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(top = spacing.inlineGap, bottom = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        SettingsSection(stringResource(R.string.settings_sound_accent), Modifier.selectableGroup()) {
            ChoiceRow(
                label = Accent.AMERICAN.label(),
                selected = accent == Accent.AMERICAN,
                onSelect = { onAction(SoundAction.OnAccentPick(Accent.AMERICAN)) }
            )
            RowDivider()
            ChoiceRow(
                label = Accent.BRITISH.label(),
                selected = accent == Accent.BRITISH,
                onSelect = { onAction(SoundAction.OnAccentPick(Accent.BRITISH)) },
                supporting = when {
                    british == null || hasBritish -> null
                    accent == Accent.BRITISH -> stringResource(R.string.settings_accent_british_fallback)
                    else -> stringResource(R.string.settings_accent_british_missing)
                },
                // Kept selectable while chosen, so the fallback it explains stays visible and can be undone.
                enabled = hasBritish || accent == Accent.BRITISH || british == null
            )
        }
        SettingsSection(stringResource(R.string.settings_sound_speed), Modifier.selectableGroup()) {
            Text(
                text = stringResource(R.string.settings_sound_speed_detail),
                modifier = margin.padding(bottom = spacing.inlineGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SpeechSpeed.entries.forEachIndexed { index, speed ->
                if (index > 0) RowDivider()
                ChoiceRow(
                    label = speed.label(),
                    selected = state.speed == speed,
                    onSelect = { onAction(SoundAction.OnSpeedPick(speed)) }
                )
            }
        }
        if (state.voices != VoicesUi.NoEngine) {
            SecondaryActionButton(
                text = stringResource(R.string.settings_sound_try),
                onClick = { onAction(SoundAction.OnPlayClick) },
                modifier = margin,
                enabled = !state.isPlaying
            )
        }
        SettingsSection(stringResource(R.string.settings_sound_voices)) {
            VoiceRows(state.voices, onAction, modifier = margin)
        }
    }
}

@Composable
private fun VoiceRows(voices: VoicesUi, onAction: (SoundAction) -> Unit, modifier: Modifier = Modifier) {
    when (voices) {
        VoicesUi.Checking -> ListRow(headline = stringResource(R.string.settings_voice_checking))

        VoicesUi.NoEngine -> Text(
            text = stringResource(R.string.settings_voice_no_engine),
            modifier = modifier,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        is VoicesUi.Known -> {
            ListRow(
                headline = stringResource(R.string.settings_voice_american),
                supporting = voices.american.label()
            )
            RowDivider()
            ListRow(
                headline = stringResource(R.string.settings_voice_british),
                supporting = voices.british.label()
            )
            if (voices.canInstall) {
                SecondaryActionButton(
                    text = stringResource(R.string.settings_sound_install),
                    onClick = { onAction(SoundAction.OnInstallClick) },
                    modifier = modifier.padding(top = GilTheme.spacing.itemGap)
                )
            }
        }
    }
}

@Composable
private fun VoiceState.label(): String = stringResource(
    when (this) {
        VoiceState.INSTALLED -> R.string.settings_voice_installed
        VoiceState.NOT_INSTALLED -> R.string.settings_voice_not_installed
        VoiceState.UNSUPPORTED -> R.string.settings_voice_unsupported
    }
)

@PreviewLightDark
@Composable
private fun SoundScreenPreview() {
    GilTheme {
        Surface {
            SoundScreen(
                state = SoundState(
                    accent = Accent.AMERICAN,
                    voices = VoicesUi.Known(VoiceState.INSTALLED, VoiceState.NOT_INSTALLED)
                ),
                onAction = {},
                onBack = {}
            )
        }
    }
}
