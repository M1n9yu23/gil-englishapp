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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Mono 16-bit little-endian PCM, the format both `AudioTrack` playback and the on-device
 * recognizer's `EXTRA_AUDIO_SOURCE` read without conversion.
 *
 * [pcm] is the recorder's own buffer, handed over without a copy; treat it as read-only.
 */
class VoiceRecording(val pcm: ByteArray, val sampleRateHz: Int) {
    val duration: Duration
        get() = (pcm.size / BYTES_PER_SAMPLE * 1_000L / sampleRateHz).milliseconds

    internal companion object {
        const val BYTES_PER_SAMPLE = 2
    }
}
