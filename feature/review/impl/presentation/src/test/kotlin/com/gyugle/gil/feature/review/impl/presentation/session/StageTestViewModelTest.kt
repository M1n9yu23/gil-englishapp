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
package com.gyugle.gil.feature.review.impl.presentation.session

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageLessons
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.progress.api.FakeProgressCommands
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.review.impl.domain.ReviewDeck
import com.gyugle.gil.feature.review.impl.domain.StageTestPlanner
import com.gyugle.gil.feature.settings.api.FakeAppSettingsObserver
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class StageTestViewModelTest {
    private val s0 = StageTag(StageId("stage.s0"), 0, LocalizedText("출발", "Getting started"))
    private val a1 = StageTag(StageId("stage.a1"), 2, LocalizedText("첫걸음", "First steps"))

    /** Ten words, so a test asks all of them and allows one wrong answer. */
    private val words = (0 until 10).map { LessonItem(SenseId("sense.w$it.n.x"), "word$it", null, "뜻$it") }

    private fun lesson(id: String, items: List<LessonItem>) =
        Lesson(LessonId(id), LocalizedText(id, id), LocalizedText("", ""), emptyList(), items, next = null)

    private val course = FakeCourseObserver(
        lesson("lesson.s0-01.1", emptyList()),
        lesson("lesson.a1-01.1", words.take(5)),
        lesson("lesson.a1-01.2", words.drop(5))
    ).apply {
        stages = listOf(
            StageLessons(s0, listOf(LessonId("lesson.s0-01.1"))),
            StageLessons(a1, listOf(LessonId("lesson.a1-01.1"), LessonId("lesson.a1-01.2")))
        )
    }
    private val lexicon = FakeLexiconObserver()
    private val clock = FakeStudyClock()
    private val observer = FakeProgressObserver()
    private val commands = FakeProgressCommands(observer)
    private val speaker = FakeSpeaker()
    private val appSettings = FakeAppSettingsObserver()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(stage: String = "stage.a1", handle: SavedStateHandle = SavedStateHandle()) =
        StageTestViewModel(
            stage,
            handle,
            StageTestPlanner(course),
            observer,
            commands,
            lexicon,
            ReviewDeck(lexicon),
            speaker,
            clock,
            appSettings
        )

    private val StageTestViewModel.phase get() = state.value.phase

    private val StageTestViewModel.choice get() = (phase as ReviewPhase.Asking).card as CardUi.Choice

    /** The English the card asks for: heard, or meant by the Korean shown. */
    private fun StageTestViewModel.rightAnswer(): String = when (val prompt = choice.prompt) {
        is ChoicePromptUi.Listen -> speaker.spoken.last().text
        is ChoicePromptUi.English -> words.single { it.meaningKo == prompt.meaningKo }.text
        else -> error("Unexpected prompt $prompt")
    }

    private fun StageTestViewModel.answer(right: Boolean) {
        val answer = rightAnswer()
        val index = choice.options.indexOfFirst { (it.text == answer) == right }
        onAction(ReviewAction.OnOptionClick(index))
        onAction(ReviewAction.OnContinueClick)
    }

    @Test
    fun load_stageWithItems_introducesTheTestBeforeAsking() {
        observer.completedLessons.value = setOf(LessonId("lesson.s0-01.1"))

        val viewModel = viewModel()

        assertThat(viewModel.state.value.isStageTest).isTrue()
        assertThat(viewModel.phase).isEqualTo(
            ReviewPhase.TestIntro(
                TestIntroUi(LocalizedTextUi("첫걸음", "First steps"), questions = 10, passMark = 9, lessons = 2)
            )
        )
        assertThat(speaker.spoken).isEmpty()
    }

    @Test
    fun start_asksTheFirstQuestionByEar() {
        val viewModel = viewModel()

        viewModel.onAction(ReviewAction.OnStartTestClick)

        assertThat((viewModel.phase as ReviewPhase.Asking).number).isEqualTo(1)
        assertThat((viewModel.phase as ReviewPhase.Asking).count).isEqualTo(10)
        assertThat(viewModel.choice.prompt).isEqualTo(ChoicePromptUi.Listen(revealed = null))
    }

    @Test
    fun answer_wrong_saysTheRightAnswerButNotThatItComesBack() {
        val viewModel = viewModel()
        viewModel.onAction(ReviewAction.OnStartTestClick)
        val answer = viewModel.rightAnswer()

        viewModel.onAction(ReviewAction.OnOptionClick(viewModel.choice.options.indexOfFirst { it.text != answer }))

        val feedback = (viewModel.phase as ReviewPhase.Asking).feedback
        assertThat(feedback).isEqualTo(FeedbackUi(GradeUi.WRONG, note = viewModel.noteFor(answer), comesBack = false))
        assertThat(commands.reviews).isEmpty()
        assertThat(commands.mistakes).isEmpty()
    }

    private fun StageTestViewModel.noteFor(answer: String): String? =
        words.single { it.text == answer }.meaningKo.takeIf { choice.prompt is ChoicePromptUi.Listen }

    @Test
    fun finish_oneWrong_passesAndSkipsEveryLessonLeftUpToTheStage() {
        observer.completedLessons.value = setOf(LessonId("lesson.s0-01.1"))
        val viewModel = viewModel()
        viewModel.onAction(ReviewAction.OnStartTestClick)

        viewModel.answer(right = false)
        repeat(9) { viewModel.answer(right = true) }

        val result = (viewModel.phase as ReviewPhase.TestFinished).result
        assertThat(result.passed).isTrue()
        assertThat(result.right).isEqualTo(9)
        assertThat(result.answered).isEqualTo(10)
        assertThat(result.lessons).isEqualTo(2)
        assertThat(result.missed.size).isEqualTo(1)
        val (lessons, tested) = commands.skipped.single()
        assertThat(lessons).containsExactly(LessonId("lesson.a1-01.1"), LessonId("lesson.a1-01.2"))
        assertThat(tested.size).isEqualTo(10)
        assertThat(tested.values.count { it == ReviewRating.AGAIN }).isEqualTo(1)
    }

    @Test
    fun continue_secondWrongAnswer_endsTheTestWithoutRecordingIt() {
        val viewModel = viewModel()
        viewModel.onAction(ReviewAction.OnStartTestClick)

        viewModel.answer(right = true)
        viewModel.answer(right = false)
        viewModel.answer(right = false)

        val result = (viewModel.phase as ReviewPhase.TestFinished).result
        assertThat(result.passed).isFalse()
        assertThat(result.answered).isEqualTo(3)
        assertThat(result.endedEarly).isTrue()
        assertThat(result.missed.size).isEqualTo(2)
        assertThat(commands.skipped).isEmpty()
    }

    @Test
    fun finish_recordingFails_showsTheErrorAndRetryRecordsIt() {
        commands.completeError = DataError.Local.DISK_FULL
        val viewModel = viewModel()
        viewModel.onAction(ReviewAction.OnStartTestClick)
        repeat(10) { viewModel.answer(right = true) }
        assertThat(viewModel.phase).isEqualTo(ReviewPhase.Failed)

        commands.completeError = null
        viewModel.onAction(ReviewAction.OnRetryClick)

        assertThat((viewModel.phase as ReviewPhase.TestFinished).result.passed).isTrue()
        assertThat(commands.skipped.size).isEqualTo(1)
    }

    @Test
    fun load_stageWithTooFewItems_fails() {
        assertThat(viewModel(stage = "stage.s0").phase).isEqualTo(ReviewPhase.Failed)
    }

    @Test
    fun load_contentUnavailable_failsAndRetryLoads() {
        course.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat(viewModel.phase).isEqualTo(ReviewPhase.Failed)

        course.error = null
        viewModel.onAction(ReviewAction.OnRetryClick)

        assertThat(viewModel.phase).isInstanceOf(ReviewPhase.TestIntro::class)
    }

    @Test
    fun restore_afterProcessDeath_goesOnWithTheSameQuestions() {
        val handle = SavedStateHandle()
        val first = viewModel(handle = handle)
        first.onAction(ReviewAction.OnStartTestClick)
        first.answer(right = true)
        val second = first.rightAnswer()

        val restored = viewModel(handle = handle)

        assertThat((restored.phase as ReviewPhase.Asking).number).isEqualTo(2)
        assertThat(restored.rightAnswer()).isEqualTo(second)
    }

    @Test
    fun restore_afterAPassWasRecorded_doesNotRecordItAgain() {
        val handle = SavedStateHandle()
        val first = viewModel(handle = handle)
        first.onAction(ReviewAction.OnStartTestClick)
        repeat(10) { first.answer(right = true) }
        observer.completedLessons.value = setOf(LessonId("lesson.a1-01.1"), LessonId("lesson.a1-01.2"))

        val restored = viewModel(handle = handle)

        assertThat((restored.phase as ReviewPhase.TestFinished).result.lessons).isEqualTo(3)
        assertThat(commands.skipped.size).isEqualTo(1)
    }

    @Test
    fun close_midTest_recordsTheTimeAndLeaves() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ReviewAction.OnStartTestClick)
        viewModel.onAction(ReviewAction.OnResumed)
        clock.advanceBy(2.minutes)

        viewModel.events.test {
            viewModel.onAction(ReviewAction.OnCloseClick)

            assertThat(awaitItem()).isEqualTo(ReviewEvent.Close)
        }
        assertThat(commands.studyTime).containsExactly(2.minutes)
        assertThat(commands.skipped).isEmpty()
    }
}
