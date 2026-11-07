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
package com.gyugle.gil.core.speech

sealed interface SpeakerStatus {
    /** Binding to the system's speech engine. */
    data object Connecting : SpeakerStatus

    /** No speech engine is installed, or it failed to start. Lessons fall back to text. */
    data object NoEngine : SpeakerStatus

    /** The engine is running. Each language has its own [VoiceState]. */
    data class Connected(private val voices: Map<SpeechLanguage, VoiceState>) : SpeakerStatus {
        fun voice(language: SpeechLanguage): VoiceState = voices[language] ?: VoiceState.UNSUPPORTED
    }
}
