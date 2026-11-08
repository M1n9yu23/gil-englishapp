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
package com.gyugle.gil.feature.listening.impl.data.playback

import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Measures the time sound actually plays and hands it to [record]: listening counts as study time,
 * though not as a review. Paused time, including while another app has the sound, does not count.
 */
internal class StudyTimeTracker(private val timeSource: TimeSource, private val record: (Duration) -> Unit) {
    private var playingSince: TimeMark? = null

    fun onPlayingChanged(playing: Boolean) {
        if (playing) {
            if (playingSince == null) playingSince = timeSource.markNow()
        } else {
            playingSince?.let { record(it.elapsedNow()) }
            playingSince = null
        }
    }

    /** Records the time so far and goes on counting, so a long session is not lost if the app is killed. */
    fun checkpoint() {
        val since = playingSince ?: return
        record(since.elapsedNow())
        playingSince = timeSource.markNow()
    }
}
