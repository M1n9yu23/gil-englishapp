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
import kotlin.time.Duration
import kotlinx.coroutines.CompletableDeferred

/**
 * A [VoiceRecorder] that suspends in [record] until [stop] is called or the test completes
 * [recording], then returns [result].
 */
class FakeVoiceRecorder : VoiceRecorder {
    /** One second of silence by default. */
    var result: Outcome<VoiceRecording, RecordingError> = Outcome.Success(VoiceRecording(ByteArray(32_000), 16_000))

    var recording: CompletableDeferred<Unit>? = null
        private set
    var lastMaxDuration: Duration? = null
        private set

    override suspend fun record(maxDuration: Duration): Outcome<VoiceRecording, RecordingError> {
        lastMaxDuration = maxDuration
        val inProgress = CompletableDeferred<Unit>()
        recording = inProgress
        try {
            inProgress.await()
        } finally {
            recording = null
        }
        return result
    }

    override fun stop() {
        recording?.complete(Unit)
    }
}
