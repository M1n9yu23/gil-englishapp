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

import androidx.compose.runtime.Immutable
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.VoiceState

/** Which accents can play. With neither, the page shows no sound buttons. */
@Immutable
data class VoicesUi(val american: Boolean = false, val british: Boolean = false) {
    val any: Boolean get() = american || british
}

internal fun SpeakerStatus.toVoicesUi(): VoicesUi = when (this) {
    is SpeakerStatus.Connected -> VoicesUi(
        american = voice(SpeechLanguage.AMERICAN_ENGLISH) == VoiceState.INSTALLED,
        british = voice(SpeechLanguage.BRITISH_ENGLISH) == VoiceState.INSTALLED
    )

    SpeakerStatus.Connecting, SpeakerStatus.NoEngine -> VoicesUi()
}
