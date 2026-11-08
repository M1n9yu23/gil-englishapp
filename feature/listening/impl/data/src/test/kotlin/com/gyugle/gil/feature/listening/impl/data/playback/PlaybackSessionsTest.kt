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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.feature.listening.impl.domain.model.Clip
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackSession
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import com.gyugle.gil.feature.listening.impl.domain.model.TrackInfo
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

class PlaybackSessionsTest {
    private val tracks = listOf(
        RenderedTrack("pattern.hi", "Hi.", "안녕", listOf(Clip.Silence(1.seconds))),
        RenderedTrack("pattern.bye", "Bye.", "잘 가", listOf(Clip.Silence(1.seconds)))
    )
    private val playing = PlayerReading(
        idle = false,
        ended = false,
        itemCount = 2,
        index = 1,
        playWhenReady = true,
        suppressed = false
    )

    @Test
    fun sessionOf_playing_isTheCurrentTrackOfTheQueue() {
        assertThat(sessionOf(playing, PlaylistSource.Recent, tracks)).isEqualTo(
            PlaybackSession(
                source = PlaylistSource.Recent,
                tracks = listOf(TrackInfo("pattern.hi", "Hi.", "안녕"), TrackInfo("pattern.bye", "Bye.", "잘 가")),
                index = 1,
                playing = true,
                ended = false
            )
        )
    }

    @Test
    fun sessionOf_heldBackForAnotherAppsSound_isNotPlaying() {
        assertThat(sessionOf(playing.copy(suppressed = true), PlaylistSource.Recent, tracks)?.playing).isEqualTo(false)
    }

    @Test
    fun sessionOf_ended_isNotPlaying() {
        val session = sessionOf(playing.copy(ended = true), PlaylistSource.Recent, tracks)

        assertThat(session?.playing).isEqualTo(false)
        assertThat(session?.ended).isEqualTo(true)
    }

    @Test
    fun sessionOf_idleEmptyOrNoQueue_isNothing() {
        assertThat(sessionOf(playing.copy(idle = true), PlaylistSource.Recent, tracks)).isNull()
        assertThat(sessionOf(playing.copy(itemCount = 0), PlaylistSource.Recent, tracks)).isNull()
        assertThat(sessionOf(playing, source = null, tracks = tracks)).isNull()
    }

    @Test
    fun lastPlayedOf_resumesAtTheCurrentTrackUntilTheEnd() {
        val session = checkNotNull(sessionOf(playing, PlaylistSource.Dialogues, tracks))

        assertThat(lastPlayedOf(session)).isEqualTo(LastPlayed(PlaylistSource.Dialogues, "pattern.bye"))
        assertThat(lastPlayedOf(session.copy(ended = true))).isNull()
    }

    @Test
    fun resumePointMoved_onlyWhenTheTrackSourceOrEndChanges() {
        val session = checkNotNull(sessionOf(playing, PlaylistSource.Recent, tracks))

        assertThat(resumePointMoved(null, session)).isEqualTo(true)
        assertThat(resumePointMoved(session, session.copy(playing = false))).isEqualTo(false)
        assertThat(resumePointMoved(session, session.copy(index = 0))).isEqualTo(true)
        assertThat(resumePointMoved(session, session.copy(source = PlaylistSource.DueToday))).isEqualTo(true)
        assertThat(resumePointMoved(session, session.copy(ended = true))).isEqualTo(true)
    }
}
