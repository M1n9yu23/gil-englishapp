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

import androidx.compose.runtime.Immutable
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.SpeechSpeed

/** @property accent null until the settings are read; [speed] is read with it. */
@Immutable
data class SoundState(
    val accent: Accent? = null,
    val speed: SpeechSpeed = SpeechSpeed.NORMAL,
    val voices: VoicesUi = VoicesUi.Checking,
    val isPlaying: Boolean = false
)

@Immutable
sealed interface VoicesUi {
    data object Checking : VoicesUi

    data object NoEngine : VoicesUi

    data class Known(val american: VoiceState, val british: VoiceState) : VoicesUi {
        val canInstall: Boolean get() = american == VoiceState.NOT_INSTALLED || british == VoiceState.NOT_INSTALLED
    }
}

sealed interface SoundAction {
    data class OnAccentPick(val accent: Accent) : SoundAction

    data class OnSpeedPick(val speed: SpeechSpeed) : SoundAction

    data object OnPlayClick : SoundAction

    data object OnInstallClick : SoundAction

    /** No app answered the install request. */
    data object OnInstallUnavailable : SoundAction

    /** Back from the speech engine's settings, where voices may have been installed. */
    data object OnResumed : SoundAction
}

sealed interface SoundEvent {
    data object OpenVoiceInstall : SoundEvent

    data class ShowMessage(val message: UiText) : SoundEvent
}
