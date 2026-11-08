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
@file:OptIn(UnstableApi::class)

package com.gyugle.gil.feature.listening.impl.data.playback

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.FileDataSource
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.source.SilenceMediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import com.gyugle.gil.feature.listening.impl.domain.model.Clip
import java.io.File

/**
 * Builds each track as one source: its WAV files and silences joined in order. The player sees a
 * track as one item, so "next" skips a whole word with its meaning and examples.
 */
internal class TrackSourceFactory(private val queue: TrackQueue) : MediaSource.Factory {
    private val files = ProgressiveMediaSource.Factory(FileDataSource.Factory())

    override fun createMediaSource(mediaItem: MediaItem): MediaSource {
        // The session only lets in items the queue holds.
        val track = checkNotNull(queue.track(mediaItem.mediaId)) { "No track ${mediaItem.mediaId} in the queue" }
        val builder = ConcatenatingMediaSource2.Builder().setMediaItem(mediaItem)
        track.clips.forEach { clip ->
            when (clip) {
                is Clip.Audio -> builder.add(
                    files.createMediaSource(MediaItem.fromUri(Uri.fromFile(File(clip.path)))),
                    // A progressive source needs a length before it opens; the header gave it.
                    clip.duration.inWholeMilliseconds.coerceAtLeast(1)
                )

                is Clip.Silence -> builder.add(
                    SilenceMediaSource(clip.duration.inWholeMicroseconds),
                    clip.duration.inWholeMilliseconds
                )
            }
        }
        return builder.build()
    }

    override fun setDrmSessionManagerProvider(
        drmSessionManagerProvider: DrmSessionManagerProvider
    ): MediaSource.Factory = this

    override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory =
        this

    override fun getSupportedTypes(): IntArray = intArrayOf(C.CONTENT_TYPE_OTHER)
}
