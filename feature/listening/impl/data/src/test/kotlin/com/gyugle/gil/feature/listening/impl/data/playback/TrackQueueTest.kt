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

import android.app.Application
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.gyugle.gil.feature.listening.impl.domain.model.Clip
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import kotlin.time.Duration.Companion.seconds
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Media items carry Android URIs, so these run on Robolectric. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TrackQueueTest {
    private val track = RenderedTrack("pattern.hi", "Hi.", "안녕", listOf(Clip.Silence(1.seconds)))
    private val queue = TrackQueue().apply { load(PlaylistSource.Recent, listOf(track)) }

    @Test
    fun playableItem_knownId_carriesItsTitleAndAPlayableUri() {
        val item = queue.playableItem("pattern.hi")

        assertThat(item?.mediaMetadata?.title).isEqualTo("Hi.")
        assertThat(item?.mediaMetadata?.artist).isEqualTo("안녕")
        assertThat(item?.localConfiguration).isNotNull()
    }

    @Test
    fun playableItem_idNotInTheQueue_isNull() {
        // A controller cannot make the player open a file by sending its path as an id.
        assertThat(queue.playableItem("file:///sdcard/other.wav")).isNull()
    }

    @Test
    fun createMediaSource_track_joinsItsClipsUnderTheTrackItem() {
        val item = checkNotNull(queue.playableItem("pattern.hi"))

        val source = TrackSourceFactory(queue).createMediaSource(item)

        assertThat(source.mediaItem.mediaId).isEqualTo("pattern.hi")
    }
}
