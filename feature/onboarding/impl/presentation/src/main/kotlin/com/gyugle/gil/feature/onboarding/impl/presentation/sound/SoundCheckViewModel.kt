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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.onboarding.impl.domain.repository.OnboardingRepository
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Plays the word once the voice is ready, then asks whether the learner heard it. Saying "no"
 * survives process death; whether the word already played does too, so it does not play again.
 * Finishing ends the first run, and the app then leaves it on its own.
 */
@HiltViewModel
class SoundCheckViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val speaker: Speaker,
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private data class Local(val isFinishing: Boolean = false, val error: UiText? = null)

    private val local = MutableStateFlow(Local())
    private val notHeard = savedStateHandle.getStateFlow(KEY_NOT_HEARD, false)

    val state: StateFlow<SoundCheckState> = combine(speaker.status, notHeard, local) { status, notHeard, local ->
        SoundCheckState(
            phase = phaseOf(status, notHeard),
            isFinishing = local.isFinishing,
            error = local.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SoundCheckState())

    private val eventChannel = Channel<SoundCheckEvent>()
    val events: Flow<SoundCheckEvent> = eventChannel.receiveAsFlow()

    private var speakJob: Job? = null

    init {
        viewModelScope.launch {
            speaker.status.first { it.hasAmericanVoice() }
            if (savedStateHandle.get<Boolean>(KEY_PLAYED) != true) play()
        }
    }

    fun onAction(action: SoundCheckAction) {
        when (action) {
            SoundCheckAction.OnPlayClick -> {
                savedStateHandle[KEY_NOT_HEARD] = false
                play()
            }

            SoundCheckAction.OnHeardClick, SoundCheckAction.OnContinueWithoutSoundClick -> finish()

            SoundCheckAction.OnNotHeardClick -> savedStateHandle[KEY_NOT_HEARD] = true

            SoundCheckAction.OnInstallClick -> viewModelScope.launch {
                eventChannel.send(SoundCheckEvent.OpenVoiceInstall)
            }

            SoundCheckAction.OnInstallUnavailable ->
                local.update { it.copy(error = UiText.StringResource(R.string.onboarding_sound_install_unavailable)) }

            SoundCheckAction.OnResumed -> speaker.refresh()
        }
    }

    private fun play() {
        savedStateHandle[KEY_PLAYED] = true
        speakJob?.cancel()
        speakJob = viewModelScope.launch {
            local.update { it.copy(error = null) }
            // A voice that fails to speak sounds the same to the learner as one they cannot hear.
            speaker.speak(WORD, SpeechLanguage.AMERICAN_ENGLISH).onFailure { savedStateHandle[KEY_NOT_HEARD] = true }
        }
    }

    private fun finish() {
        if (local.value.isFinishing) return
        speakJob?.cancel()
        viewModelScope.launch {
            local.update { it.copy(isFinishing = true, error = null) }
            onboardingRepository.complete()
                .onFailure { error -> local.update { it.copy(isFinishing = false, error = error.toUiText()) } }
        }
    }

    companion object {
        /** The first word of the course, and short enough to hear at once. */
        const val WORD = "Hello"

        private const val KEY_NOT_HEARD = "not_heard"
        private const val KEY_PLAYED = "played"
        private const val STOP_TIMEOUT_MILLIS = 5_000L

        private fun SpeakerStatus.hasAmericanVoice(): Boolean =
            this is SpeakerStatus.Connected && voice(SpeechLanguage.AMERICAN_ENGLISH) == VoiceState.INSTALLED

        private fun phaseOf(status: SpeakerStatus, notHeard: Boolean): SoundCheckPhase = when {
            status is SpeakerStatus.Connecting -> SoundCheckPhase.Checking
            status is SpeakerStatus.NoEngine -> SoundCheckPhase.NoEngine
            !status.hasAmericanVoice() -> SoundCheckPhase.NoVoice
            notHeard -> SoundCheckPhase.NotHeard
            else -> SoundCheckPhase.Ready
        }
    }
}
