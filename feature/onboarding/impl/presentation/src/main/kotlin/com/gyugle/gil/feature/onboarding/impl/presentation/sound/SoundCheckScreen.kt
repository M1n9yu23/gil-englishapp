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
package com.gyugle.gil.feature.onboarding.impl.presentation.sound

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.tts.TextToSpeech
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.feature.onboarding.impl.presentation.OnboardingSteps
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingPrimaryButton
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingStepLayout

private object SoundCheckTokens {
    /** The screen margin less a text button's 12dp inner padding. */
    val TextButtonStart = 8.dp
}

@Composable
fun SoundCheckRoot(onBack: () -> Unit, viewModel: SoundCheckViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SoundCheckEvent.OpenVoiceInstall -> try {
                context.startActivity(Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA))
            } catch (_: ActivityNotFoundException) {
                viewModel.onAction(SoundCheckAction.OnInstallUnavailable)
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(SoundCheckAction.OnResumed) }
    SoundCheckScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun SoundCheckScreen(
    state: SoundCheckState,
    onAction: (SoundCheckAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (title, body) = state.phase.texts()
    val enabled = !state.isFinishing
    OnboardingStepLayout(
        title = stringResource(title),
        body = stringResource(body),
        modifier = modifier,
        progress = OnboardingSteps.sound,
        onBack = onBack,
        error = state.error,
        stepContent = {
            if (state.phase ==
                SoundCheckPhase.Ready
            ) {
                Word(onPlay = { onAction(SoundCheckAction.OnPlayClick) })
            }
        }
    ) {
        when (state.phase) {
            SoundCheckPhase.Checking, SoundCheckPhase.Ready -> {
                OnboardingPrimaryButton(
                    text = stringResource(R.string.onboarding_sound_heard),
                    onClick = { onAction(SoundCheckAction.OnHeardClick) },
                    enabled = enabled && state.phase == SoundCheckPhase.Ready
                )
                SecondaryActionButton(
                    text = stringResource(R.string.onboarding_sound_not_heard),
                    onClick = { onAction(SoundCheckAction.OnNotHeardClick) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled && state.phase == SoundCheckPhase.Ready
                )
            }

            SoundCheckPhase.NotHeard -> {
                OnboardingPrimaryButton(
                    text = stringResource(R.string.onboarding_sound_play_again),
                    onClick = { onAction(SoundCheckAction.OnPlayClick) },
                    enabled = enabled
                )
                InstallButton(onAction, enabled, primary = false)
                ContinueWithoutSoundButton(onAction, enabled)
            }

            SoundCheckPhase.NoVoice -> {
                InstallButton(onAction, enabled, primary = true)
                ContinueWithoutSoundButton(onAction, enabled)
            }

            SoundCheckPhase.NoEngine -> OnboardingPrimaryButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = { onAction(SoundCheckAction.OnContinueWithoutSoundClick) },
                enabled = enabled
            )
        }
    }
}

private fun SoundCheckPhase.texts(): Pair<Int, Int> = when (this) {
    SoundCheckPhase.Checking -> R.string.onboarding_sound_title to R.string.onboarding_sound_checking
    SoundCheckPhase.Ready -> R.string.onboarding_sound_title to R.string.onboarding_sound_body
    SoundCheckPhase.NotHeard -> R.string.onboarding_sound_not_heard_title to R.string.onboarding_sound_not_heard_body
    SoundCheckPhase.NoVoice -> R.string.onboarding_sound_no_voice_title to R.string.onboarding_sound_no_voice_body
    SoundCheckPhase.NoEngine -> R.string.onboarding_sound_no_engine_title to R.string.onboarding_sound_no_engine_body
}

/** The word being played, in the English typeface, with the way to hear it again. */
@Composable
private fun Word(onPlay: () -> Unit) {
    val spacing = GilTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)) {
        EnglishText(
            text = SoundCheckViewModel.WORD,
            modifier = Modifier.padding(horizontal = spacing.screenMargin),
            size = EnglishTextSize.Focus
        )
        // The button's own inner padding brings its text to the screen margin.
        TextActionButton(
            text = stringResource(R.string.onboarding_sound_play_again),
            onClick = onPlay,
            modifier = Modifier.padding(start = SoundCheckTokens.TextButtonStart)
        )
    }
}

@Composable
private fun InstallButton(onAction: (SoundCheckAction) -> Unit, enabled: Boolean, primary: Boolean) {
    val text = stringResource(R.string.onboarding_sound_install)
    val onClick = { onAction(SoundCheckAction.OnInstallClick) }
    if (primary) {
        OnboardingPrimaryButton(text = text, onClick = onClick, enabled = enabled)
    } else {
        SecondaryActionButton(text = text, onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = enabled)
    }
}

@Composable
private fun ContinueWithoutSoundButton(onAction: (SoundCheckAction) -> Unit, enabled: Boolean) {
    TextActionButton(
        text = stringResource(R.string.onboarding_sound_continue_without),
        onClick = { onAction(SoundCheckAction.OnContinueWithoutSoundClick) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled
    )
}

@PreviewLightDark
@Composable
private fun SoundCheckScreenPreview() {
    GilTheme {
        Surface {
            SoundCheckScreen(state = SoundCheckState(phase = SoundCheckPhase.Ready), onAction = {}, onBack = {})
        }
    }
}
