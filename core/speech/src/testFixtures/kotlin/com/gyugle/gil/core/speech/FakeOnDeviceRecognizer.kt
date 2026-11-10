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
import kotlinx.coroutines.CompletableDeferred

/**
 * An [OnDeviceRecognizer] that answers [recognize] with [heard]. Set [pending] to hold the answer
 * until the test completes it.
 */
class FakeOnDeviceRecognizer(var availability: RecognizerAvailability = RecognizerAvailability.READY) :
    OnDeviceRecognizer {
    var heard: Outcome<String, RecognitionError> = Outcome.Success("")
    var pending: CompletableDeferred<Unit>? = null
    val recognized = mutableListOf<VoiceRecording>()
    var availabilityChecks = 0
        private set

    override suspend fun availability(language: SpeechLanguage): RecognizerAvailability {
        availabilityChecks++
        return availability
    }

    override suspend fun recognize(
        recording: VoiceRecording,
        language: SpeechLanguage
    ): Outcome<String, RecognitionError> {
        recognized += recording
        pending?.await()
        return heard
    }
}
