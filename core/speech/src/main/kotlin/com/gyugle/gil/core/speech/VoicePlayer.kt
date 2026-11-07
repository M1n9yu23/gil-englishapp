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

/** Plays a [VoiceRecording] back, so the learner can compare it with the voice they copied. */
interface VoicePlayer {
    /**
     * Plays [recording] and suspends until it has played. Cancelling the call stops it. A later
     * call waits for this one. Returns false when the device could not play it.
     */
    suspend fun play(recording: VoiceRecording): Boolean
}
