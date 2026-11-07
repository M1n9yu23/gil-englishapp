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
import kotlin.time.Duration.Companion.seconds

/**
 * Records the learner's voice into memory. Nothing is written to disk.
 *
 * The caller asks for `RECORD_AUDIO` first; without it [record] fails with
 * [RecordingError.PERMISSION_DENIED]. From Android 10, while another app holds the microphone the
 * system delivers silence instead of an error, so a recording can be silent.
 */
interface VoiceRecorder {
    /**
     * Records until [stop] is called or [maxDuration] has passed, then returns the audio.
     * Cancelling the call discards it. A second call waits for the first to finish.
     *
     * @param maxDuration more than zero and at most [MAX_DURATION]
     */
    suspend fun record(maxDuration: Duration): Outcome<VoiceRecording, RecordingError>

    /** Ends the recording in progress early. Does nothing when nothing is being recorded. */
    fun stop()

    companion object {
        /** About 2 MB of audio, held in memory. */
        val MAX_DURATION: Duration = 60.seconds
    }
}
