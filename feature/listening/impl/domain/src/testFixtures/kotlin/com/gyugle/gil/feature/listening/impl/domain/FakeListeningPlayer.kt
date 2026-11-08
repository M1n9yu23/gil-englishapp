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
package com.gyugle.gil.feature.listening.impl.domain

import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackError
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackSession
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import com.gyugle.gil.feature.listening.impl.domain.model.TrackInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Plays nothing: [play] loads a session at once, and the controls move it as a player would.
 * [error], when set, fails [play]. A test ends playback by setting [session], and fails it by
 * emitting to [failures].
 */
class FakeListeningPlayer : ListeningPlayer {
    override val session = MutableStateFlow<PlaybackSession?>(null)
    override val failures = MutableSharedFlow<PlaybackError>(extraBufferCapacity = 1)
    val played = mutableListOf<Triple<PlaylistSource, List<RenderedTrack>, Int>>()
    var error: PlaybackError? = null

    override suspend fun play(
        source: PlaylistSource,
        tracks: List<RenderedTrack>,
        startIndex: Int
    ): EmptyOutcome<PlaybackError> {
        error?.let { return Outcome.Failure(it) }
        played += Triple(source, tracks, startIndex)
        session.value = PlaybackSession(
            source = source,
            tracks = tracks.map { TrackInfo(it.id, it.title, it.subtitle) },
            index = startIndex,
            playing = true,
            ended = false
        )
        return Outcome.Success(Unit)
    }

    override fun pause() = change { it.copy(playing = false) }

    override fun resume() = change { it.copy(playing = true, ended = false, index = if (it.ended) 0 else it.index) }

    override fun skipToNext() = change { it.copy(index = (it.index + 1).coerceAtMost(it.tracks.lastIndex)) }

    override fun skipToPrevious() = change { it }

    override fun stop() {
        session.value = null
    }

    private fun change(transform: (PlaybackSession) -> PlaybackSession) {
        session.value = session.value?.let(transform)
    }
}
