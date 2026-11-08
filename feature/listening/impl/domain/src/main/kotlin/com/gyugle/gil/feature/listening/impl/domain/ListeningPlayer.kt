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
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackError
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackSession
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Plays a playlist in the background, so it goes on with the screen off or the app closed. The lock
 * screen, the notification and headphone buttons control the same playback, and the state here
 * follows them.
 */
interface ListeningPlayer {
    /** What is loaded, null when nothing is. */
    val session: StateFlow<PlaybackSession?>

    /** Each time playback stops on an error; [session] then becomes null. */
    val failures: Flow<PlaybackError>

    /** Replaces what is loaded with [tracks] of [source] and plays from [startIndex]. */
    suspend fun play(source: PlaylistSource, tracks: List<RenderedTrack>, startIndex: Int): EmptyOutcome<PlaybackError>

    fun pause()

    fun resume()

    fun skipToNext()

    /** Back to the start of the current track, as a player's previous button does early in a track. */
    fun skipToPrevious()

    /** Stops and unloads the playlist. */
    fun stop()
}
