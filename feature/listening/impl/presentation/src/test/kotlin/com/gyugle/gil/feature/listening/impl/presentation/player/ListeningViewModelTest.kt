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
package com.gyugle.gil.feature.listening.impl.presentation.player

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.api.UnitLessons
import com.gyugle.gil.feature.listening.impl.domain.FakeListeningPlayer
import com.gyugle.gil.feature.listening.impl.domain.FakeListeningPreferences
import com.gyugle.gil.feature.listening.impl.domain.FakeTrackRenderer
import com.gyugle.gil.feature.listening.impl.domain.ListeningLibrary
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.PauseLength
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackError
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderError
import com.gyugle.gil.feature.listening.impl.domain.model.SpokenLanguage
import com.gyugle.gil.feature.listening.impl.domain.model.TrackPart
import com.gyugle.gil.feature.listening.impl.presentation.R
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ListeningViewModelTest {
    private val stage = StageTag(StageId("stage.a1"), 2, LocalizedText("첫걸음", "First steps"))
    private val hi = PatternId("pattern.hi")
    private val niceToMeet = PatternId("pattern.nice-to-meet-you")
    private val lesson = Lesson(
        id = LessonId("lesson.a1-01.1"),
        title = LocalizedText("인사", "Greetings"),
        goal = LocalizedText("", ""),
        steps = emptyList(),
        introduced = listOf(
            LessonItem(hi, "Hi.", null, "안녕"),
            LessonItem(niceToMeet, "Nice to meet you.", null, "반가워요")
        ),
        next = null
    )
    private val unit = UnitLessons(UnitId("unit.a1-01"), LocalizedText("인사", "Greetings"), listOf(lesson.id))

    private val course = FakeCourseObserver(lesson).apply { units = listOf(unit) }
    private val lexicon = FakeLexiconObserver(
        PatternEntry(hi, "Hi.", "안녕", null, stage, emptyList()),
        PatternEntry(niceToMeet, "Nice to meet you.", "만나서 반가워요", null, stage, emptyList())
    )
    private val progress = FakeProgressObserver().apply { completedLessons.value = setOf(lesson.id) }
    private val renderer = FakeTrackRenderer()
    private val player = FakeListeningPlayer()
    private val preferences = FakeListeningPreferences()
    private val speaker = FakeSpeaker()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): ListeningViewModel {
        val viewModel = ListeningViewModel(
            ListeningLibrary(course, lexicon, progress, FakeStudyClock()),
            renderer,
            player,
            preferences,
            speaker
        )
        // The state is shared while the screen watches it.
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        return viewModel
    }

    @Test
    fun choosing_listsSourcesWithSomethingInThemAndTheUnits() = runTest {
        val state = viewModel().state.value

        assertThat(state).isEqualTo(
            ListeningState.Choosing(
                sources = listOf(SourceUi(PlaylistSource.Recent, SourceName.Recent, 2)),
                units = listOf(
                    SourceUi(PlaylistSource.CourseUnit(unit.id), SourceName.CourseUnit("인사", "Greetings"), 2)
                ),
                resume = null,
                pace = ListeningPace(),
                koreanVoice = true
            )
        )
    }

    @Test
    fun choosing_lastPlayedSourceStillThere_offersToResumeIt() = runTest {
        preferences.lastPlayed.value = LastPlayed(PlaylistSource.CourseUnit(unit.id), niceToMeet.value)

        val state = viewModel().state.value as ListeningState.Choosing

        assertThat(state.resume?.source).isEqualTo(PlaylistSource.CourseUnit(unit.id))
    }

    @Test
    fun choosing_lastPlayedSourceNowEmpty_offersNothingToResume() = runTest {
        preferences.lastPlayed.value = LastPlayed(PlaylistSource.DueToday, hi.value)

        val state = viewModel().state.value as ListeningState.Choosing

        assertThat(state.resume).isNull()
    }

    @Test
    fun nothingFinished_isNothingYet() = runTest {
        progress.completedLessons.value = emptySet()

        assertThat(viewModel().state.value).isEqualTo(ListeningState.NothingYet)
    }

    @Test
    fun contentUnreadable_failsAndRetryReadsAgain() = runTest {
        course.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat(viewModel.state.value).isEqualTo(ListeningState.Failed)

        course.error = null
        viewModel.onAction(ListeningAction.OnRetryClick)

        assertThat(viewModel.state.value).isInstanceOf(ListeningState.Choosing::class)
    }

    @Test
    fun voices_noEngineOrNoEnglish_saySoAndOfferToInstallEnglish() = runTest {
        speaker.status.value = SpeakerStatus.NoEngine
        val viewModel = viewModel()
        assertThat(viewModel.state.value).isEqualTo(ListeningState.NoEngine)

        speaker.status.value = SpeakerStatus.Connected(mapOf(SpeechLanguage.KOREAN to VoiceState.INSTALLED))
        assertThat(viewModel.state.value).isEqualTo(ListeningState.NoEnglishVoice)

        viewModel.events.test {
            viewModel.onAction(ListeningAction.OnInstallClick)
            assertThat(awaitItem()).isEqualTo(ListeningEvent.OpenVoiceInstall)
        }
    }

    @Test
    fun noKoreanVoice_saysSoAndPlaysWithoutKorean() = runTest {
        speaker.status.value = SpeakerStatus.Connected(mapOf(SpeechLanguage.AMERICAN_ENGLISH to VoiceState.INSTALLED))
        val viewModel = viewModel()
        assertThat((viewModel.state.value as ListeningState.Choosing).koreanVoice).isEqualTo(false)

        viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))

        val parts = renderer.rendered.single().first.flatMap { it.parts }
        assertThat(parts.any { it is TrackPart.Speech && it.language == SpokenLanguage.KOREAN }).isEqualTo(false)
    }

    @Test
    fun sourceClick_rendersAtThePaceAndPlaysFromTheStart() = runTest {
        preferences.pace.value = ListeningPace(ListeningSpeed.SLOW, PauseLength.LONG)
        val viewModel = viewModel()

        viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))

        val (tracks, speed) = renderer.rendered.single()
        assertThat(speed).isEqualTo(ListeningSpeed.SLOW)
        assertThat(tracks.map { it.id }).isEqualTo(listOf(hi.value, niceToMeet.value))
        assertThat(tracks.first().parts[1]).isEqualTo(TrackPart.Pause(PauseLength.LONG.duration))
        val (source, _, start) = player.played.single()
        assertThat(source).isEqualTo(PlaylistSource.Recent)
        assertThat(start).isEqualTo(0)
        assertThat(viewModel.state.value).isEqualTo(
            ListeningState.Playing(TrackUi("Hi.", "안녕"), position = 1, count = 2, playing = true)
        )
    }

    @Test
    fun resumeClick_startsAtTheSavedTrack() = runTest {
        preferences.lastPlayed.value = LastPlayed(PlaylistSource.CourseUnit(unit.id), niceToMeet.value)
        val viewModel = viewModel()

        viewModel.onAction(ListeningAction.OnResumeClick)

        val (source, _, start) = player.played.single()
        assertThat(source).isEqualTo(PlaylistSource.CourseUnit(unit.id))
        assertThat(start).isEqualTo(1)
    }

    @Test
    fun preparing_showsProgressAndCancelGoesBackToChoosing() = runTest {
        renderer.gate = CompletableDeferred()
        val viewModel = viewModel()

        viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))
        // Two items, each with its English, Korean and two examples.
        assertThat(viewModel.state.value).isEqualTo(ListeningState.Preparing(done = 0, total = 8))

        viewModel.onAction(ListeningAction.OnCancelPreparingClick)

        assertThat(viewModel.state.value).isInstanceOf(ListeningState.Choosing::class)
        assertThat(player.played).isEqualTo(emptyList())
    }

    @Test
    fun renderFails_saysWhyAndStaysOnTheList() = runTest {
        renderer.error = RenderError.VOICE_NOT_INSTALLED
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))
            assertThat(awaitItem()).isEqualTo(
                ListeningEvent.ShowMessage(UiText.StringResource(R.string.listening_message_voice_missing))
            )
        }
        assertThat(viewModel.state.value).isInstanceOf(ListeningState.Choosing::class)
    }

    @Test
    fun playerUnavailable_saysItCouldNotStart() = runTest {
        player.error = PlaybackError.UNAVAILABLE
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))
            assertThat(awaitItem()).isEqualTo(
                ListeningEvent.ShowMessage(UiText.StringResource(R.string.listening_message_play_failed))
            )
        }
    }

    @Test
    fun playbackFails_saysItStoppedAndGoesBackToTheList() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))

        viewModel.events.test {
            player.session.value = null
            player.failures.emit(PlaybackError.FAILED)
            assertThat(awaitItem()).isEqualTo(
                ListeningEvent.ShowMessage(UiText.StringResource(R.string.listening_message_playback_failed))
            )
        }
        assertThat(viewModel.state.value).isInstanceOf(ListeningState.Choosing::class)
    }

    @Test
    fun controls_pauseResumeSkipAndStop() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))

        viewModel.onAction(ListeningAction.OnPlayPauseClick)
        assertThat((viewModel.state.value as ListeningState.Playing).playing).isEqualTo(false)

        viewModel.onAction(ListeningAction.OnPlayPauseClick)
        viewModel.onAction(ListeningAction.OnNextClick)
        assertThat(viewModel.state.value).isEqualTo(
            ListeningState.Playing(TrackUi("Nice to meet you.", "만나서 반가워요"), position = 2, count = 2, playing = true)
        )

        viewModel.onAction(ListeningAction.OnStopClick)
        assertThat(viewModel.state.value).isInstanceOf(ListeningState.Choosing::class)
    }

    @Test
    fun ended_showsHowManyAndPlaysAgainFromTheStart() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ListeningAction.OnSourceClick(PlaylistSource.Recent))
        player.session.value = player.session.value?.copy(index = 1, ended = true, playing = false)
        assertThat(viewModel.state.value).isEqualTo(ListeningState.Ended(count = 2))

        viewModel.onAction(ListeningAction.OnPlayAgainClick)

        assertThat((viewModel.state.value as ListeningState.Playing).position).isEqualTo(1)
    }

    @Test
    fun paceDialogs_openAndPickingSavesAndCloses() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ListeningAction.OnPauseLengthClick)
        assertThat((viewModel.state.value as ListeningState.Choosing).dialog).isEqualTo(PaceDialog.PAUSE)
        viewModel.onAction(ListeningAction.OnPauseLengthPick(PauseLength.SHORT))
        viewModel.onAction(ListeningAction.OnSpeedClick)
        viewModel.onAction(ListeningAction.OnSpeedPick(ListeningSpeed.SLOW))

        val state = viewModel.state.value as ListeningState.Choosing
        assertThat(state.dialog).isNull()
        assertThat(state.pace).isEqualTo(ListeningPace(ListeningSpeed.SLOW, PauseLength.SHORT))
        assertThat(preferences.pace.first()).isEqualTo(ListeningPace(ListeningSpeed.SLOW, PauseLength.SHORT))
    }

    @Test
    fun resumed_readsVoicesAgain() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ListeningAction.OnResumed)

        assertThat(speaker.refreshCount).isEqualTo(1)
    }
}
