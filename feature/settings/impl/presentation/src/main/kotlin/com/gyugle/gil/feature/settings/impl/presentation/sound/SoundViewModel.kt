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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.englishVoice
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.impl.domain.repository.SettingsRepository
import com.gyugle.gil.feature.settings.impl.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The sample plays with the chosen accent and speed, so the learner hears what lessons will sound like. */
@HiltViewModel
class SoundViewModel @Inject constructor(
    private val speaker: Speaker,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SoundState())
    val state: StateFlow<SoundState> = _state.asStateFlow()

    private val eventChannel = Channel<SoundEvent>()
    val events: Flow<SoundEvent> = eventChannel.receiveAsFlow()

    private var playJob: Job? = null

    init {
        settingsRepository.observeSettings()
            .onEach { settings -> _state.update { it.copy(accent = settings.accent, speed = settings.speed) } }
            .launchIn(viewModelScope)
        speaker.status.onEach { status -> _state.update { it.copy(voices = status.toUi()) } }.launchIn(viewModelScope)
    }

    fun onAction(action: SoundAction) {
        when (action) {
            is SoundAction.OnAccentPick -> save { settingsRepository.setAccent(action.accent) }

            is SoundAction.OnSpeedPick -> save { settingsRepository.setSpeed(action.speed) }

            SoundAction.OnPlayClick -> play()

            SoundAction.OnInstallClick -> send(SoundEvent.OpenVoiceInstall)

            SoundAction.OnInstallUnavailable -> send(
                SoundEvent.ShowMessage(UiText.StringResource(R.string.settings_sound_install_unavailable))
            )

            SoundAction.OnResumed -> speaker.refresh()
        }
    }

    private fun save(write: suspend () -> EmptyOutcome<DataError.Local>) {
        viewModelScope.launch {
            write().onFailure { error -> eventChannel.send(SoundEvent.ShowMessage(error.toUiText())) }
        }
    }

    /** A second tap starts the sample again. */
    private fun play() {
        val state = _state.value
        val accent = state.accent ?: return
        playJob?.cancel()
        playJob = viewModelScope.launch {
            _state.update { it.copy(isPlaying = true) }
            val language = speaker.status.value.englishVoice(preferBritish = accent == Accent.BRITISH)
            val rate = if (state.speed == SpeechSpeed.SLOW) SpeechRate.SLOW else SpeechRate.NORMAL
            speaker.speak(SAMPLE, language, rate).onFailure {
                eventChannel.send(SoundEvent.ShowMessage(UiText.StringResource(R.string.settings_sound_play_failed)))
            }
            _state.update { it.copy(isPlaying = false) }
        }
    }

    private fun send(event: SoundEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun SpeakerStatus.toUi(): VoicesUi = when (this) {
        SpeakerStatus.Connecting -> VoicesUi.Checking

        SpeakerStatus.NoEngine -> VoicesUi.NoEngine

        is SpeakerStatus.Connected -> VoicesUi.Known(
            american = voice(SpeechLanguage.AMERICAN_ENGLISH),
            british = voice(SpeechLanguage.BRITISH_ENGLISH)
        )
    }

    private companion object {
        /** From the first A1 unit, short enough to compare accents and speeds. */
        const val SAMPLE = "Nice to meet you."
    }
}
