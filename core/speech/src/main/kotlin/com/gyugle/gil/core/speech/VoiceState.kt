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

/** Whether the speech engine can speak one [SpeechLanguage] without a network connection. */
enum class VoiceState {
    /** An offline voice is installed. */
    INSTALLED,

    /**
     * The engine knows the language but has no offline voice for it yet. The learner can download
     * one in the engine's settings (`TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA`).
     */
    NOT_INSTALLED,

    /** The engine has no voice for the language at all. */
    UNSUPPORTED
}
