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

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The playlist the app handed to the service. A controller names tracks only by id; the service
 * looks them up here, so no controller can make the player open a file of its choosing. The app and
 * the service share one process, and both run on the main thread.
 */
@Singleton
internal class TrackQueue @Inject constructor() {
    var source: PlaylistSource? = null
        private set
    var tracks: List<RenderedTrack> = emptyList()
        private set

    fun load(source: PlaylistSource, tracks: List<RenderedTrack>) {
        this.source = source
        this.tracks = tracks
    }

    fun track(id: String): RenderedTrack? = tracks.firstOrNull { it.id == id }

    /** The item a controller asks for, or null for an id this queue does not hold. */
    fun playableItem(id: String): MediaItem? = track(id)?.let(::mediaItemOf)
}

/** The scheme of the placeholder URI on a track's item; [TrackSourceFactory] reads only the id. */
private const val TRACK_SCHEME = "gil-listening"

/** An item a controller sends: the id and what the notification shows, no URI. */
internal fun requestItemOf(track: RenderedTrack): MediaItem =
    MediaItem.Builder().setMediaId(track.id).setMediaMetadata(metadataOf(track)).build()

private fun mediaItemOf(track: RenderedTrack): MediaItem = MediaItem.Builder()
    .setMediaId(track.id)
    .setUri(Uri.Builder().scheme(TRACK_SCHEME).opaquePart(track.id).build())
    .setMediaMetadata(metadataOf(track))
    .build()

private fun metadataOf(track: RenderedTrack): MediaMetadata = MediaMetadata.Builder()
    .setTitle(track.title)
    .setArtist(track.subtitle)
    .setIsPlayable(true)
    .setIsBrowsable(false)
    .build()
