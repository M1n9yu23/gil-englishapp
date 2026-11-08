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
package com.gyugle.gil.feature.listening.impl.data

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechError
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.listening.impl.domain.model.Clip
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.RenderError
import com.gyugle.gil.feature.listening.impl.domain.model.RenderProgress
import com.gyugle.gil.feature.listening.impl.domain.model.SpokenLanguage
import com.gyugle.gil.feature.listening.impl.domain.model.Track
import com.gyugle.gil.feature.listening.impl.domain.model.TrackPart
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.FakeAppSettingsObserver
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TtsTrackRendererTest {

    @TempDir
    lateinit var tempDir: File

    private val speaker = FakeSpeaker()
    private val settings = FakeAppSettingsObserver()

    private val track = Track(
        id = "pattern.hi",
        title = "Hi.",
        subtitle = "안녕",
        parts = listOf(
            TrackPart.Speech("Hi.", SpokenLanguage.ENGLISH),
            TrackPart.Pause(3.seconds),
            TrackPart.Speech("안녕", SpokenLanguage.KOREAN),
            TrackPart.Speech("Hi, Tom.", SpokenLanguage.ENGLISH),
            TrackPart.Speech("Hi, Tom.", SpokenLanguage.ENGLISH)
        )
    )

    @Test
    fun render_writesEachDistinctPieceOnceAndPlaysTheFilesInOrder() = runTest {
        val cache = cache()

        val progress = renderer(cache).render(listOf(track), ListeningSpeed.NORMAL).toList()

        assertThat(speaker.synthesized.map { it.first }).containsExactly(
            FakeSpeaker.Utterance("Hi.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL),
            FakeSpeaker.Utterance("안녕", SpeechLanguage.KOREAN, SpeechRate.NORMAL),
            FakeSpeaker.Utterance("Hi, Tom.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL)
        )
        assertThat(progress.dropLast(1)).isEqualTo((0..3).map { RenderProgress.Writing(it, 3) })
        val clips = (progress.last() as RenderProgress.Done).tracks.single().clips
        val files = speaker.synthesized.map { it.second.path }
        // The fake engine writes empty files, which have no length to tell.
        val silent = Duration.ZERO
        assertThat(clips).isEqualTo(
            listOf(
                Clip.Audio(files[0], silent),
                Clip.Silence(3.seconds),
                Clip.Audio(files[1], silent),
                Clip.Audio(files[2], silent),
                Clip.Audio(files[2], silent)
            )
        )
    }

    @Test
    fun render_slowBritish_slowsOnlyEnglishInTheChosenAccent() = runTest {
        settings.settings.value = AppSettings(accent = Accent.BRITISH)

        renderer(cache()).render(listOf(track), ListeningSpeed.SLOW).toList()

        assertThat(speaker.synthesized.map { it.first }).containsExactly(
            FakeSpeaker.Utterance("Hi.", SpeechLanguage.BRITISH_ENGLISH, SpeechRate.SLOW),
            FakeSpeaker.Utterance("안녕", SpeechLanguage.KOREAN, SpeechRate.NORMAL),
            FakeSpeaker.Utterance("Hi, Tom.", SpeechLanguage.BRITISH_ENGLISH, SpeechRate.SLOW)
        )
    }

    @Test
    fun render_britishVoiceMissing_usesAmerican() = runTest {
        settings.settings.value = AppSettings(accent = Accent.BRITISH)
        speaker.status.value = SpeakerStatus.Connected(
            mapOf(
                SpeechLanguage.AMERICAN_ENGLISH to VoiceState.INSTALLED,
                SpeechLanguage.KOREAN to VoiceState.INSTALLED
            )
        )

        renderer(cache()).render(listOf(track), ListeningSpeed.NORMAL).toList()

        assertThat(speaker.synthesized.first().first.language).isEqualTo(SpeechLanguage.AMERICAN_ENGLISH)
    }

    @Test
    fun render_pieceAlreadyInCache_isNotWrittenAgain() = runTest {
        val cache = cache()
        cache.prepare()
        cache.fileFor(SpeechKey("Hi.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL)).writeBytes(ByteArray(100))

        renderer(cache).render(listOf(track), ListeningSpeed.NORMAL).toList()

        assertThat(speaker.synthesized.map { it.first.text }).containsExactly("안녕", "Hi, Tom.")
    }

    @Test
    fun render_noEngine_failsWithoutWriting() = runTest {
        speaker.status.value = SpeakerStatus.NoEngine

        val progress = renderer(cache()).render(listOf(track), ListeningSpeed.NORMAL).toList()

        assertThat(progress).containsExactly(RenderProgress.Failed(RenderError.NO_ENGINE))
        assertThat(speaker.synthesized).isEqualTo(emptyList())
    }

    @Test
    fun render_engineFails_stopsWithItsError() = runTest {
        speaker.error = SpeechError.VOICE_NOT_INSTALLED

        val progress = renderer(cache()).render(listOf(track), ListeningSpeed.NORMAL).toList()

        assertThat(progress.last()).isEqualTo(RenderProgress.Failed(RenderError.VOICE_NOT_INSTALLED))
        assertThat(speaker.synthesized.size).isEqualTo(1)
    }

    @Test
    fun render_waitsForTheEngineToConnect() = runTest {
        speaker.status.value = SpeakerStatus.Connecting
        val renderer = renderer(cache())

        val result = async { renderer.render(listOf(track), ListeningSpeed.NORMAL).toList() }
        testScheduler.advanceUntilIdle()
        speaker.status.value = SpeakerStatus.Connected(SpeechLanguage.entries.associateWith { VoiceState.INSTALLED })

        assertThat(result.await().last()).isInstanceOf(RenderProgress.Done::class)
    }

    private fun TestScope.cache() = SpeechCache({ File(tempDir, "listening") }, StandardTestDispatcher(testScheduler))

    private fun renderer(cache: SpeechCache) = TtsTrackRenderer(speaker, settings, cache)
}
