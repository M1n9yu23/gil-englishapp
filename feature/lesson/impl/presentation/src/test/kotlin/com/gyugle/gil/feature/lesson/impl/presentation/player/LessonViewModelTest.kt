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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.FakeVoicePlayer
import com.gyugle.gil.core.speech.FakeVoiceRecorder
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.feature.curriculum.api.Choice
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PairWord
import com.gyugle.gil.feature.curriculum.api.PassageQuestion
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import com.gyugle.gil.feature.lesson.impl.domain.repository.FakeLessonRunRepository
import com.gyugle.gil.feature.progress.api.FakeProgressCommands
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.ReviewRating
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LessonViewModelTest {
    private val f = SoundId("sound.f")
    private val v = SoundId("sound.v")
    private val fan = SenseId("sense.fan.n.cooler")

    private val lesson = Lesson(
        id = LessonId("lesson.s1-07.1"),
        title = LocalizedText("f와 v", "f and v"),
        goal = LocalizedText("f와 v를 구별할 수 있어요", "Tell f from v"),
        steps = listOf(
            LessonStep.Listen(Sentence(1, "Fan.", "부채.")),
            LessonStep.ChooseMeaning(
                prompt = Sentence(2, "fan", "부채"),
                options = listOf(
                    Choice("부채", english = false, correct = true, why = null),
                    Choice("밴", english = false, correct = false, why = "`van`이 밴이에요.")
                ),
                item = fan
            ),
            LessonStep.MinimalPair(
                listOf(PairWord("fan", "fæn"), PairWord("van", "væn")),
                answer = 1,
                why = "v예요.",
                item = v
            ),
            LessonStep.Spell(answer = "five", ko = "다섯", ipa = "faɪv", item = f),
            LessonStep.ReadPassage(
                sentences = listOf(Sentence(3, "A fan.", "부채 하나.")),
                questions = listOf(
                    PassageQuestion(
                        "무엇인가요?",
                        listOf(Choice("부채", false, true, null), Choice("밴", false, false, "아니에요."))
                    ),
                    PassageQuestion(
                        "몇 개인가요?",
                        listOf(Choice("하나", false, true, null), Choice("둘", false, false, "a는 하나예요."))
                    )
                )
            )
        ),
        introduced = listOf(
            LessonItem(f, "f, ph", "f", null),
            LessonItem(v, "v", "v", null),
            LessonItem(fan, "fan", "fæn", "부채")
        ),
        next = LessonId("lesson.s1-07.2")
    )

    private val course = FakeCourseObserver(lesson)
    private val runs = FakeLessonRunRepository()
    private val progress = FakeProgressCommands()
    private val speaker = FakeSpeaker()
    private val recorder = FakeVoiceRecorder()
    private val player = FakeVoicePlayer()
    private val clock = FakeStudyClock()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        LessonViewModel(lesson.id.value, handle, course, runs, progress, speaker, recorder, player, clock)

    private val LessonViewModel.playing get() = state.value.phase as LessonPhase.Playing

    private fun LessonViewModel.pickText(text: String) {
        val options = (playing.step as StepUi.Choice).options
        onAction(LessonAction.OnOptionClick(options.indexOfFirst { it.text == text }))
    }

    private fun LessonViewModel.continueStep() = onAction(LessonAction.OnContinueClick)

    @Test
    fun load_newLesson_showsTheFirstStepAndPlaysIt() {
        val viewModel = viewModel()

        assertThat(viewModel.playing.stepNumber).isEqualTo(1)
        assertThat(viewModel.playing.stepCount).isEqualTo(5)
        assertThat(viewModel.playing.step).isEqualTo(StepUi.Listen(SentenceUi("Fan.", "부채."), isMeaningShown = false))
        assertThat(speaker.spoken.single()).isEqualTo(
            FakeSpeaker.Utterance("Fan.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL)
        )
        assertThat(viewModel.state.value.title).isEqualTo(LocalizedTextUi("f와 v", "f and v"))
    }

    @Test
    fun load_missingLesson_failsWithoutRetry() {
        course.lessons.clear()

        val phase = viewModel().state.value.phase as LessonPhase.Failed

        assertThat(phase.error.canRetry).isFalse()
    }

    @Test
    fun load_unreadableContent_retryLoadsIt() {
        course.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat((viewModel.state.value.phase as LessonPhase.Failed).error.canRetry).isTrue()

        course.error = null
        viewModel.onAction(LessonAction.OnRetryLoadClick)

        assertThat(viewModel.state.value.phase).isInstanceOf<LessonPhase.Playing>()
    }

    @Test
    fun choice_rightAnswer_revealsItWithoutQueueingAgain() {
        val viewModel = viewModel()
        viewModel.continueStep()

        viewModel.pickText("부채")

        val step = viewModel.playing.step as StepUi.Choice
        assertThat(step.correct).isEqualTo(step.picked)
        assertThat(viewModel.playing.feedback?.grade).isEqualTo(GradeUi.CORRECT)
        assertThat(viewModel.playing.stepCount).isEqualTo(5)
        assertThat(progress.mistakes).isEmpty()
    }

    @Test
    fun choice_optionsBeforeAnswering_giveNothingAway() {
        val viewModel = viewModel()
        viewModel.continueStep()

        assertThat((viewModel.playing.step as StepUi.Choice).correct).isNull()
    }

    @Test
    fun choice_wrongFirstTry_explainsRecordsTheMistakeAndQueuesTheStepAgain() {
        val viewModel = viewModel()
        viewModel.continueStep()

        viewModel.pickText("밴")

        assertThat(viewModel.playing.feedback).isEqualTo(
            FeedbackUi(GradeUi.WRONG, why = "`van`이 밴이에요.", comesBack = true)
        )
        assertThat(viewModel.playing.stepCount).isEqualTo(6)
        assertThat(progress.mistakes).containsExactly(fan)
        assertThat(runs.runs.getValue(lesson.id).queue).isEqualTo(listOf(0, 1, 2, 3, 4, 1))
    }

    @Test
    fun choice_secondClickAfterAnswering_changesNothing() {
        val viewModel = viewModel()
        viewModel.continueStep()
        viewModel.pickText("밴")
        val answered = viewModel.state.value

        viewModel.pickText("부채")

        assertThat(viewModel.state.value).isEqualTo(answered)
    }

    @Test
    fun minimalPair_entered_playsOnlyTheAnswerWord() {
        val viewModel = viewModel()
        viewModel.continueStep()
        viewModel.pickText("부채")
        speaker.spoken.clear()

        viewModel.continueStep()

        assertThat(speaker.spoken.map { it.text }).containsExactly("van")
        assertThat((viewModel.playing.step as StepUi.Choice).options.map { it.ipa }).isEqualTo(listOf("fæn", "væn"))
    }

    @Test
    fun spell_oneLetterOff_closeAndShowsTheSpelling() {
        val viewModel = viewModel()
        repeat(2) { viewModel.continueStep() }
        viewModel.pickText("부채")
        viewModel.continueStep()
        viewModel.pickText("van")
        viewModel.continueStep()
        assertThat(viewModel.playing.canCheck).isFalse()

        viewModel.onAction(LessonAction.OnTextChange("fiev"))
        assertThat(viewModel.playing.canCheck).isTrue()
        viewModel.onAction(LessonAction.OnCheckClick)

        assertThat(viewModel.playing.feedback).isEqualTo(FeedbackUi(GradeUi.CLOSE, answer = "five"))
        assertThat(viewModel.playing.canCheck).isFalse()
    }

    @Test
    fun passage_gradedOnlyAfterTheLastQuestion() {
        val viewModel = playToPassage()

        viewModel.pickText("밴")
        assertThat(runs.runs.getValue(lesson.id).outcomes[4]).isNull()
        assertThat(viewModel.playing.feedback?.comesBack).isEqualTo(false)

        viewModel.continueStep()
        val prompt = (viewModel.playing.step as StepUi.Choice).prompt as ChoicePrompt.Passage
        assertThat(prompt.questionNumber).isEqualTo(2)
        assertThat(prompt.showsTranslations).isFalse()

        viewModel.pickText("하나")
        val answered = (viewModel.playing.step as StepUi.Choice).prompt as ChoicePrompt.Passage
        assertThat(answered.showsTranslations).isTrue()
        assertThat(runs.runs.getValue(lesson.id).outcomes.getValue(4).first).isEqualTo(AnswerGrade.WRONG)
        assertThat(viewModel.playing.feedback?.comesBack).isEqualTo(true)
    }

    @Test
    fun finish_allRight_savesGoodRatingsAndClearsThePlace() {
        val viewModel = playToPassage()
        viewModel.pickText("부채")
        viewModel.continueStep()
        viewModel.pickText("하나")

        viewModel.continueStep()

        val result = (viewModel.state.value.phase as LessonPhase.Finished).result
        assertThat(result.isSaving).isFalse()
        assertThat(result.saveFailed).isFalse()
        assertThat(result.hasNext).isTrue()
        assertThat(result.items.map { it.comesBackSoon }).isEqualTo(listOf(false, false, false))
        assertThat(progress.completed.single()).isEqualTo(
            lesson.id to mapOf(f to ReviewRating.GOOD, v to ReviewRating.GOOD, fan to ReviewRating.GOOD)
        )
        assertThat(runs.runs[lesson.id]).isNull()
    }

    @Test
    fun finish_missedStepRightOnTheRetry_hardAndMarkedComingBack() {
        val viewModel = viewModel()
        viewModel.continueStep()
        viewModel.pickText("밴")
        viewModel.continueStep()
        viewModel.pickText("van")
        viewModel.continueStep()
        viewModel.onAction(LessonAction.OnTextChange("five"))
        viewModel.onAction(LessonAction.OnCheckClick)
        viewModel.continueStep()
        viewModel.pickText("부채")
        viewModel.continueStep()
        viewModel.pickText("하나")
        viewModel.continueStep()

        assertThat(viewModel.playing.isAgain).isTrue()
        assertThat(viewModel.playing.stepNumber).isEqualTo(6)
        viewModel.pickText("부채")
        assertThat(viewModel.playing.feedback?.comesBack).isEqualTo(false)
        viewModel.continueStep()

        val result = (viewModel.state.value.phase as LessonPhase.Finished).result
        assertThat(result.items.map { it.comesBackSoon }).isEqualTo(listOf(false, false, true))
        assertThat(progress.completed.single().second.getValue(fan)).isEqualTo(ReviewRating.HARD)
    }

    @Test
    fun finish_saveFails_offersRetryAndKeepsThePlace() {
        progress.completeError = DataError.Local.DISK_FULL
        val viewModel = playToPassage()
        viewModel.pickText("부채")
        viewModel.continueStep()
        viewModel.pickText("하나")
        viewModel.continueStep()

        assertThat((viewModel.state.value.phase as LessonPhase.Finished).result.saveFailed).isTrue()
        assertThat(runs.runs[lesson.id]).isNotNull()

        progress.completeError = null
        viewModel.onAction(LessonAction.OnSaveRetryClick)

        assertThat((viewModel.state.value.phase as LessonPhase.Finished).result.saveFailed).isFalse()
        assertThat(progress.completed.size).isEqualTo(1)
    }

    @Test
    fun open_runLeftHalfway_asksAndContinuesWhereItStopped() {
        runs.runs[lesson.id] = LessonRun.start(5).next().answer(AnswerGrade.CORRECT).next()
        val viewModel = viewModel()

        assertThat(viewModel.state.value.phase).isEqualTo(LessonPhase.AskResume(step = 3, count = 5))

        viewModel.onAction(LessonAction.OnResumeClick)

        assertThat(viewModel.playing.stepNumber).isEqualTo(3)
        assertThat((viewModel.playing.step as StepUi.Choice).prompt).isEqualTo(ChoicePrompt.SoundPair)
    }

    @Test
    fun open_runLeftHalfway_startOverBeginsAtTheTop() {
        runs.runs[lesson.id] = LessonRun.start(5).next().answer(AnswerGrade.WRONG).next()
        val viewModel = viewModel()

        viewModel.onAction(LessonAction.OnRestartClick)

        assertThat(viewModel.playing.stepNumber).isEqualTo(1)
        assertThat(viewModel.playing.stepCount).isEqualTo(5)
    }

    @Test
    fun open_runOfOlderContent_startsFresh() {
        runs.runs[lesson.id] = LessonRun.start(9).next().next()

        assertThat(viewModel().playing.stepNumber).isEqualTo(1)
    }

    @Test
    fun open_afterProcessDeath_goesStraightBackWithTheTypedText() {
        runs.runs[lesson.id] = LessonRun.start(5).next().answer(AnswerGrade.CORRECT).next().next()
        val handle = SavedStateHandle(mapOf("started" to true, "text" to "fiv"))

        val viewModel = viewModel(handle)

        assertThat(viewModel.playing.stepNumber).isEqualTo(4)
        assertThat((viewModel.playing.step as StepUi.Typed).text).isEqualTo("fiv")
    }

    @Test
    fun open_afterProcessDeathOnAnAnsweredStep_movesOn() {
        runs.runs[lesson.id] = LessonRun.start(5).next().answer(AnswerGrade.CORRECT)

        val viewModel = viewModel(SavedStateHandle(mapOf("started" to true)))

        assertThat(viewModel.playing.stepNumber).isEqualTo(3)
    }

    @Test
    fun noVoice_skipsSoundOnlyStepsShowsTextAndTellsOnce() {
        speaker.status.value = SpeakerStatus.NoEngine
        val viewModel = viewModel()

        assertThat(viewModel.playing.showsNoVoiceNotice).isTrue()
        assertThat(viewModel.playing.hasVoice).isFalse()
        assertThat(viewModel.playing.stepCount).isEqualTo(4)
        viewModel.continueStep()
        assertThat(viewModel.playing.showsNoVoiceNotice).isFalse()
        viewModel.pickText("부채")
        viewModel.continueStep()

        assertThat(viewModel.playing.step).isInstanceOf<StepUi.Typed>()
        assertThat(speaker.spoken).isEmpty()
    }

    @Test
    fun close_whilePlaying_asksFirstThenSavesAndCloses() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(LessonAction.OnResumed)
        viewModel.continueStep()
        clock.now += 90.seconds

        viewModel.events.test {
            viewModel.onAction(LessonAction.OnCloseClick)
            assertThat(viewModel.state.value.isExitConfirmShown).isTrue()
            expectNoEvents()

            viewModel.onAction(LessonAction.OnExitConfirm)

            assertThat(awaitItem()).isEqualTo(LessonEvent.Close)
        }
        assertThat(runs.runs.getValue(lesson.id).position).isEqualTo(1)
        assertThat(runs.runs.getValue(lesson.id).studiedMillis).isEqualTo(90_000)
        assertThat(progress.studyTime).containsExactly(90.seconds)
    }

    @Test
    fun studyTime_countsOnlyWhileOnScreen() {
        val viewModel = viewModel()
        viewModel.onAction(LessonAction.OnResumed)
        clock.now += 2.minutes
        viewModel.onAction(LessonAction.OnPaused)
        clock.now += 30.minutes
        viewModel.onAction(LessonAction.OnResumed)
        clock.now += 1.minutes
        viewModel.onAction(LessonAction.OnPaused)

        assertThat(progress.studyTime).containsExactly(2.minutes, 1.minutes)
        assertThat(runs.runs.getValue(lesson.id).studiedMillis).isEqualTo(3.minutes.inWholeMilliseconds)
    }

    @Test
    fun shadow_recordedThenPlayed_playsTheRecording() {
        val shadowLesson = lesson.copy(steps = listOf(LessonStep.Shadow(Sentence(1, "Five.", "다섯."))))
        course.lessons[0] = shadowLesson
        val viewModel = viewModel()
        viewModel.onAction(LessonAction.OnMicrophoneState(PermissionState.GRANTED))

        viewModel.onAction(LessonAction.OnRecordClick)
        assertThat((viewModel.playing.step as StepUi.Shadow).recorder.isRecording).isTrue()
        viewModel.onAction(LessonAction.OnStopRecordingClick)
        viewModel.onAction(LessonAction.OnPlayMineClick)

        val recorderUi = (viewModel.playing.step as StepUi.Shadow).recorder
        assertThat(recorderUi.hasRecording).isTrue()
        assertThat(recorderUi.isRecording).isFalse()
        assertThat(player.played.size).isEqualTo(1)
    }

    @Test
    fun shadow_microphoneNotGranted_asksForItInsteadOfRecording() = runTest {
        course.lessons[0] = lesson.copy(steps = listOf(LessonStep.Shadow(Sentence(1, "Five.", null))))
        val viewModel = viewModel()
        viewModel.onAction(LessonAction.OnMicrophoneState(PermissionState.NOT_DETERMINED))

        viewModel.events.test {
            viewModel.onAction(LessonAction.OnRecordClick)
            expectNoEvents()
            viewModel.onAction(LessonAction.OnAllowMicrophoneClick)
            assertThat(awaitItem()).isEqualTo(LessonEvent.RequestMicrophone)
        }
        assertThat(recorder.recording).isNull()
    }

    @Test
    fun result_next_opensTheNextLesson() = runTest {
        val viewModel = playToPassage()
        viewModel.pickText("부채")
        viewModel.continueStep()
        viewModel.pickText("하나")
        viewModel.continueStep()

        viewModel.events.test {
            viewModel.onAction(LessonAction.OnNextLessonClick)
            assertThat(awaitItem()).isEqualTo(LessonEvent.OpenLesson(LessonId("lesson.s1-07.2")))
        }
    }

    @Test
    fun result_afterProcessDeathOnceSaved_showsItAgainWithoutSavingTwice() {
        val handle = SavedStateHandle(
            mapOf(
                "started" to true,
                "result_minutes" to 7,
                "result_ratings" to arrayListOf("sound.f=GOOD", "sound.v=AGAIN", "sense.fan.n.cooler=GOOD"),
                "result_saved" to true
            )
        )

        val result = (viewModel(handle).state.value.phase as LessonPhase.Finished).result

        assertThat(result.minutes).isEqualTo(7)
        assertThat(result.isSaving).isFalse()
        assertThat(result.items.map { it.comesBackSoon }).isEqualTo(listOf(false, true, false))
        assertThat(progress.completed).isEmpty()
    }

    @Test
    fun result_afterProcessDeathBeforeSaving_savesTheSameRatings() {
        val handle = SavedStateHandle(
            mapOf("started" to true, "result_minutes" to 3, "result_ratings" to arrayListOf("sound.v=HARD"))
        )

        viewModel(handle)

        assertThat(progress.completed.single())
            .isEqualTo(lesson.id to mapOf<StudyItemId, ReviewRating>(v to ReviewRating.HARD))
    }

    @Test
    fun finish_allAnswered_keepsTheResultForProcessDeath() {
        val handle = SavedStateHandle()
        val viewModel = playToPassage(handle)
        viewModel.pickText("부채")
        viewModel.continueStep()
        viewModel.pickText("하나")
        viewModel.continueStep()

        assertThat(handle.get<Int>("result_minutes")).isEqualTo(1)
        assertThat(handle.get<Boolean>("result_saved")).isEqualTo(true)
    }

    @Test
    fun voiceTurnsOutMissing_onASoundPair_movesPastIt() {
        speaker.status.value = SpeakerStatus.Connecting
        runs.runs[lesson.id] = LessonRun.start(5).next().answer(AnswerGrade.CORRECT).next()
        val viewModel = viewModel(SavedStateHandle(mapOf("started" to true)))
        assertThat((viewModel.playing.step as StepUi.Choice).prompt).isEqualTo(ChoicePrompt.SoundPair)

        speaker.status.value = SpeakerStatus.NoEngine

        assertThat(viewModel.playing.step).isInstanceOf<StepUi.Typed>()
        assertThat(viewModel.playing.showsNoVoiceNotice).isTrue()
    }

    @Test
    fun restart_thenResumeAndRestartAgain_staysOnTheNewRun() {
        runs.runs[lesson.id] = LessonRun.start(5).next().answer(AnswerGrade.CORRECT).next()
        val viewModel = viewModel()

        viewModel.onAction(LessonAction.OnRestartClick)
        viewModel.onAction(LessonAction.OnResumeClick)
        viewModel.onAction(LessonAction.OnRestartClick)

        assertThat(viewModel.playing.stepNumber).isEqualTo(1)
    }

    @Test
    fun save_failing_keepsTheLessonGoing() {
        runs.error = DataError.Local.DISK_FULL
        val viewModel = viewModel()

        viewModel.continueStep()
        viewModel.pickText("밴")

        assertThat(viewModel.playing.feedback?.grade).isEqualTo(GradeUi.WRONG)
        assertThat(progress.mistakes).containsExactly(fan)
    }

    @Test
    fun minutesOf_roundsUpAndNeverZero() {
        assertThat(minutesOf(0)).isEqualTo(1)
        assertThat(minutesOf(60_000)).isEqualTo(1)
        assertThat(minutesOf(60_001)).isEqualTo(2)
    }

    /** Answers every step before the passage right. */
    private fun playToPassage(handle: SavedStateHandle = SavedStateHandle()): LessonViewModel {
        val viewModel = viewModel(handle)
        viewModel.continueStep()
        viewModel.pickText("부채")
        viewModel.continueStep()
        viewModel.pickText("van")
        viewModel.continueStep()
        viewModel.onAction(LessonAction.OnTextChange("five"))
        viewModel.onAction(LessonAction.OnCheckClick)
        viewModel.continueStep()
        return viewModel
    }
}
