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

import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackSession
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import com.gyugle.gil.feature.listening.impl.domain.model.TrackInfo

/** What the player reports, read off a Media3 `Player`, kept apart so the rules below are testable. */
internal data class PlayerReading(
    val idle: Boolean,
    val ended: Boolean,
    val itemCount: Int,
    val index: Int,
    val playWhenReady: Boolean,
    /** Held back by the system, as while another app has the sound for a moment. */
    val suppressed: Boolean
)

/**
 * The session the screen shows, or null when nothing is loaded: the player is idle, which is also
 * where it lands after an error, or has no items, or the queue was never filled in this process.
 */
internal fun sessionOf(reading: PlayerReading, source: PlaylistSource?, tracks: List<RenderedTrack>): PlaybackSession? {
    if (reading.idle || reading.itemCount == 0 || source == null) return null
    return PlaybackSession(
        source = source,
        tracks = tracks.map { TrackInfo(it.id, it.title, it.subtitle) },
        index = reading.index,
        playing = reading.playWhenReady && !reading.suppressed && !reading.ended,
        ended = reading.ended
    )
}

/** Where to resume [session] later: its current track, or nothing once it was heard to the end. */
internal fun lastPlayedOf(session: PlaybackSession): LastPlayed? =
    if (session.ended) null else session.tracks.getOrNull(session.index)?.let { LastPlayed(session.source, it.id) }

/** Whether the resume point moved from [previous] to [current], so it needs saving. */
internal fun resumePointMoved(previous: PlaybackSession?, current: PlaybackSession): Boolean = previous == null ||
    previous.source != current.source ||
    previous.index != current.index ||
    previous.ended != current.ended
