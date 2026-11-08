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
package com.gyugle.gil.feature.listening.impl.domain.model

import com.gyugle.gil.core.domain.RootError

/**
 * A loaded playlist. [index] is the current track in [tracks]. [playing] is false while paused,
 * including while another app has the sound for a moment or after the headphones came out.
 */
data class PlaybackSession(
    val source: PlaylistSource,
    val tracks: List<TrackInfo>,
    val index: Int,
    val playing: Boolean,
    val ended: Boolean
)

data class TrackInfo(val id: String, val title: String, val subtitle: String?)

enum class PlaybackError : RootError {
    /** The playback service could not be reached. */
    UNAVAILABLE,

    /** Playback stopped on an error, such as an audio file the system cleared. */
    FAILED
}
