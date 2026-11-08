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

import com.gyugle.gil.feature.listening.impl.domain.model.Clip
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.RenderError
import com.gyugle.gil.feature.listening.impl.domain.model.RenderProgress
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import com.gyugle.gil.feature.listening.impl.domain.model.Track
import com.gyugle.gil.feature.listening.impl.domain.model.TrackPart
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Renders every speech part as a file named after its text, reporting one step per part. [error],
 * when set, fails after the first part. [gate], when set, holds the render after its first step
 * until the test completes it, to observe the preparing state.
 */
class FakeTrackRenderer : TrackRenderer {
    val rendered = mutableListOf<Pair<List<Track>, ListeningSpeed>>()
    var error: RenderError? = null
    var gate: CompletableDeferred<Unit>? = null

    override fun render(tracks: List<Track>, speed: ListeningSpeed): Flow<RenderProgress> = flow {
        rendered += tracks to speed
        val total = tracks.sumOf { track -> track.parts.count { it is TrackPart.Speech } }
        emit(RenderProgress.Writing(0, total))
        gate?.await()
        error?.let {
            emit(RenderProgress.Failed(it))
            return@flow
        }
        emit(RenderProgress.Writing(total, total))
        emit(
            RenderProgress.Done(
                tracks.map { track ->
                    RenderedTrack(
                        track.id,
                        track.title,
                        track.subtitle,
                        track.parts.map { part ->
                            when (part) {
                                is TrackPart.Speech -> Clip.Audio("/cache/${part.text}.wav", 1.seconds)
                                is TrackPart.Pause -> Clip.Silence(part.duration)
                            }
                        }
                    )
                }
            )
        )
    }
}
