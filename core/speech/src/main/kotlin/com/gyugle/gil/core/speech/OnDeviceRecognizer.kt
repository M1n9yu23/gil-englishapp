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

import com.gyugle.gil.core.domain.Outcome

/**
 * The device's own speech recognizer, used to score shadowing.
 *
 * Only the on-device recognizer is used; the default one may send audio to a server.
 */
interface OnDeviceRecognizer {
    suspend fun availability(language: SpeechLanguage): RecognizerAvailability

    /**
     * What the recognizer heard in [recording], as one line of text. The audio is handed over
     * from memory and is not written to storage.
     */
    suspend fun recognize(recording: VoiceRecording, language: SpeechLanguage): Outcome<String, RecognitionError>
}
