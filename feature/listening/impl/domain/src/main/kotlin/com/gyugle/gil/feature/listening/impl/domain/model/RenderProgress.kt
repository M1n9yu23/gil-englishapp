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
import kotlin.time.Duration

sealed interface RenderProgress {
    /** [done] of [total] pieces of speech are written. Pieces written by an earlier call count as done. */
    data class Writing(val done: Int, val total: Int) : RenderProgress

    data class Done(val tracks: List<RenderedTrack>) : RenderProgress

    data class Failed(val error: RenderError) : RenderProgress
}

enum class RenderError : RootError {
    /** The phone has no speech engine, or it did not start. */
    NO_ENGINE,

    /** The engine has no offline voice for a language the tracks speak. */
    VOICE_NOT_INSTALLED,

    /** The engine failed, or the files could not be written. */
    FAILED
}

/** A [Track] ready to play: its [clips] in order. */
data class RenderedTrack(val id: String, val title: String, val subtitle: String?, val clips: List<Clip>)

sealed interface Clip {
    /** A WAV file on the device, which plays for [duration]. */
    data class Audio(val path: String, val duration: Duration) : Clip

    data class Silence(val duration: Duration) : Clip
}
