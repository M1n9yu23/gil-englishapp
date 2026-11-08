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
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundEntry
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.FakeProgressCommands
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.Mistake
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.review.impl.domain.ReviewDeck
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

class ReviewViewModelTest {
    private val stage = StageTag(StageId("stage.s1"), 1, LocalizedText("소리와 글자", "Sounds and letters"))
    private val one = SenseId("sense.one.num.count")
    private val two = SenseId("sense.two.num.count")
    private val three = SenseId("sense.three.num.count")
    private val four = SenseId("sense.four.num.count")
    private val letterA = SoundId("sound.letter-a")

    private val lexicon = FakeLexiconObserver(
        word("one", one, "1, 하나"),
        word("two", two, "2, 둘"),
        word("three", three, "3, 셋"),
        word("four", four, "4, 넷"),
        SoundEntry(letterA, "eɪ", "A a", "이름은 에이", stage, emptyList())
    )
    private val clock = FakeStudyClock()
    private val today = clock.today()
    private val observer = FakeProgressObserver()
    private val commands = FakeProgressCommands(observer).apply { nextDueDay = today + 2 }
    private val speaker = FakeSpeaker()
    private val appSettings = FakeAppSettingsObserver()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun word(text: String, id: SenseId, ko: String) = WordEntry(
        id = WordId("word.$text"),
        text = text,
        ipa = text,
        hangul = null,
        senses = listOf(WordSense(id, PartOfSpeech.NUMBER, ko, null, null, stage, emptyList())),
        collocations = emptyList(),
        family = emptyList(),
        notes = emptyList()
    )

    /** New cards, so each is asked by its meaning; the first is the longest overdue. */
    private fun due(vararg ids: StudyItemId) {
        observer.cards.value = ids.mapIndexed { index, id ->
            CardState(
                id,
                1.0,
                dueDay = today - (ids.size - index),
                lastReviewDay = today - 5,
                reviewCount = 1,
                lapseCount = 0
            )
        }
    }

    private fun viewModel(limit: Int = 20, handle: SavedStateHandle = SavedStateHandle()) =
        ReviewViewModel(limit, handle, observer, commands, lexicon, ReviewDeck(lexicon), speaker, clock, appSettings)

    private val ReviewViewModel.asking get() = state.value.phase as ReviewPhase.Asking

    private val ReviewViewModel.choice get() = asking.card as CardUi.Choice

    private fun ReviewViewModel.pick(text: String) =
        onAction(ReviewAction.OnOptionClick(choice.options.indexOfFirst { it.text == text }))

    private fun ReviewViewModel.pickWrong(right: String) =
        onAction(ReviewAction.OnOptionClick(choice.options.indexOfFirst { it.text != right }))

    @Test
    fun load_dueCards_asksTheLongestOverdueFirstAndPlaysIt() {
        due(one, two)

        val viewModel = viewModel()

        assertThat(viewModel.asking.number).isEqualTo(1)
        assertThat(viewModel.asking.count).isEqualTo(2)
        assertThat(viewModel.asking.kind).isEqualTo(CardKindUi.CHOOSE_MEANING)
        assertThat(viewModel.choice.prompt).isEqualTo(ChoicePromptUi.Meaning("one", "one", speakable = true))
        assertThat(viewModel.choice.correct).isNull()
        assertThat(speaker.spoken).containsExactly(
            FakeSpeaker.Utterance("one", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL)
        )
    }

    @Test
    fun load_moreDueThanTheLimit_asksOnlyTheLimit() {
        due(one, two, three)

        assertThat(viewModel(limit = 2).asking.count).isEqualTo(2)
    }

    @Test
    fun load_nothingDue_saysWhenTheNextReviewIs() {
        observer.cards.value = listOf(CardState(one, 3.0, today + 3, today, 1, 0))

        assertThat(viewModel().state.value.phase).isEqualTo(ReviewPhase.Empty(nextInDays = 3))
    }

    @Test
    fun load_noCardsAtAll_emptyWithoutANextDay() {
        assertThat(viewModel().state.value.phase).isEqualTo(ReviewPhase.Empty(nextInDays = null))
    }

    @Test
    fun load_dictionaryUnavailable_failsAndRetryAsks() {
        due(one, two)
        lexicon.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat(viewModel.state.value.phase).isEqualTo(ReviewPhase.Failed)

        lexicon.error = null
        viewModel.onAction(ReviewAction.OnRetryClick)

        assertThat(viewModel.state.value.phase).isInstanceOf<ReviewPhase.Asking>()
    }

    @Test
    fun answer_right_recordsGoodAndTakesItOffTheMistakes() {
        due(one, two)
        observer.mistakes.value = listOf(Mistake(one, count = 2, lastDay = today - 3))
        val viewModel = viewModel()

        viewModel.pick("1, 하나")

        assertThat(viewModel.asking.feedback).isEqualTo(FeedbackUi(GradeUi.CORRECT))
        assertThat(viewModel.choice.correct).isEqualTo(viewModel.choice.picked)
        assertThat(commands.reviews).containsExactly(one to ReviewRating.GOOD)
        assertThat(commands.clearedMistakes).containsExactly(one)
        assertThat(observer.mistakes.value).isEmpty()
    }

    @Test
    fun answer_wrong_recordsAgainAndTheMistakeAndShowsTheRightOption() {
        due(one, two)
        val viewModel = viewModel()

        viewModel.pickWrong(right = "1, 하나")

        assertThat(viewModel.asking.feedback?.grade).isEqualTo(GradeUi.WRONG)
        assertThat(viewModel.choice.options[viewModel.choice.correct!!].text).isEqualTo("1, 하나")
        assertThat(commands.reviews).containsExactly(one to ReviewRating.AGAIN)
        assertThat(commands.mistakes).containsExactly(one)
        assertThat(commands.clearedMistakes).isEmpty()
    }

    @Test
    fun answer_secondTapAfterAnswering_changesNothing() {
        due(one, two)
        val viewModel = viewModel()
        viewModel.pick("1, 하나")

        viewModel.pickWrong(right = "1, 하나")

        assertThat(commands.reviews).containsExactly(one to ReviewRating.GOOD)
    }

    @Test
    fun answer_typedOneLetterOff_recordsHardAndLeavesTheMistakes() {
        observer.cards.value = listOf(CardState(four, 30.0, today - 1, today - 30, reviewCount = 5, lapseCount = 0))
        val viewModel = viewModel()
        assertThat(viewModel.asking.kind).isEqualTo(CardKindUi.SPELL_BY_EAR)
        assertThat(viewModel.asking.canCheck).isEqualTo(false)

        viewModel.onAction(ReviewAction.OnTextChange("foru"))
        viewModel.onAction(ReviewAction.OnCheckClick)

        assertThat(viewModel.asking.feedback).isEqualTo(FeedbackUi(GradeUi.CLOSE, answer = "four"))
        assertThat(commands.reviews).containsExactly(four to ReviewRating.HARD)
        assertThat(commands.mistakes).isEmpty()
        assertThat(commands.clearedMistakes).isEmpty()
    }

    @Test
    fun finish_countsRightAnswersListsMissesAndTheNextDay() {
        due(one, two)
        val viewModel = viewModel()
        viewModel.pick("1, 하나")
        viewModel.onAction(ReviewAction.OnContinueClick)
        viewModel.pickWrong(right = "2, 둘")
        viewModel.onAction(ReviewAction.OnContinueClick)

        assertThat(viewModel.state.value.phase).isEqualTo(
            ReviewPhase.Finished(
                ResultUi(
                    answered = 2,
                    right = 1,
                    again = listOf(ResultItemUi("two", "2, 둘", detailIsIpa = false)),
                    remaining = 0,
                    nextInDays = 2
                )
            )
        )
    }

    @Test
    fun finish_moreStillDue_keepReviewingStartsWithTheRest() {
        due(one, two)
        val viewModel = viewModel(limit = 1)
        viewModel.pick("1, 하나")
        viewModel.onAction(ReviewAction.OnContinueClick)
        assertThat((viewModel.state.value.phase as ReviewPhase.Finished).result.remaining).isEqualTo(1)

        viewModel.onAction(ReviewAction.OnKeepReviewingClick)

        assertThat(viewModel.asking.number).isEqualTo(1)
        assertThat(viewModel.asking.count).isEqualTo(1)
        assertThat(viewModel.choice.prompt).isEqualTo(ChoicePromptUi.Meaning("two", "two", speakable = true))
    }

    @Test
    fun processDeath_afterAnswering_goesOnWithTheNextCardAndKeepsTheRating() {
        due(one, two)
        val handle = SavedStateHandle()
        viewModel(handle = handle).pickWrong(right = "1, 하나")

        val restored = viewModel(handle = handle)

        assertThat(restored.asking.number).isEqualTo(2)
        assertThat(restored.choice.prompt).isEqualTo(ChoicePromptUi.Meaning("two", "two", speakable = true))
        restored.pick("2, 둘")
        restored.onAction(ReviewAction.OnContinueClick)
        val result = (restored.state.value.phase as ReviewPhase.Finished).result
        assertThat(result.answered).isEqualTo(2)
        assertThat(result.again.map { it.text }).containsExactly("one")
    }

    @Test
    fun processDeath_whileTyping_keepsTheText() {
        observer.cards.value = listOf(CardState(four, 30.0, today - 1, today - 30, reviewCount = 5, lapseCount = 0))
        val handle = SavedStateHandle()
        viewModel(handle = handle).onAction(ReviewAction.OnTextChange("fo"))

        val restored = viewModel(handle = handle)

        assertThat((restored.asking.card as CardUi.Typed).text).isEqualTo("fo")
    }

    @Test
    fun load_noVoice_asksTheLetterByItsNameAndPlaysNothing() {
        speaker.status.value = SpeakerStatus.Connected(emptyMap())
        observer.cards.value = listOf(CardState(letterA, 1.0, today - 1, today - 3, 1, 0))

        val viewModel = viewModel()

        assertThat(viewModel.asking.kind).isEqualTo(CardKindUi.CHOOSE_LETTER_BY_NAME)
        assertThat(viewModel.choice.prompt).isEqualTo(ChoicePromptUi.Letter("eɪ", showsIpa = true))
        viewModel.onAction(ReviewAction.OnPlayClick)
        assertThat(speaker.spoken).isEmpty()
    }

    @Test
    fun pause_recordsTheTimeSpentOnCards() {
        due(one, two)
        val viewModel = viewModel()
        viewModel.onAction(ReviewAction.OnResumed)
        clock.advanceBy(2.minutes)

        viewModel.onAction(ReviewAction.OnPaused)

        assertThat(commands.studyTime).containsExactly(2.minutes)
    }

    @Test
    fun continue_tappedTwice_movesOnByOneCard() {
        due(one, two, three)
        val viewModel = viewModel()
        viewModel.pick("1, 하나")

        viewModel.onAction(ReviewAction.OnContinueClick)
        viewModel.onAction(ReviewAction.OnContinueClick)

        assertThat(viewModel.asking.number).isEqualTo(2)
        assertThat(viewModel.choice.prompt).isEqualTo(ChoicePromptUi.Meaning("two", "two", speakable = true))
    }

    @Test
    fun close_midSession_recordsTheTimeBeforeLeaving() = runTest {
        due(one, two)
        val viewModel = viewModel()
        viewModel.onAction(ReviewAction.OnResumed)
        clock.advanceBy(3.minutes)

        viewModel.events.test {
            viewModel.onAction(ReviewAction.OnCloseClick)

            assertThat(awaitItem()).isEqualTo(ReviewEvent.Close)
        }
        assertThat(commands.studyTime).containsExactly(3.minutes)
    }

    @Test
    fun finish_cardThatCannotBeAsked_isPassedOverAndNotLeft() {
        // A sound with nothing to tell it apart from: no other sound or letter learned.
        lexicon.entries += SoundEntry(SoundId("sound.f"), "f", "f, ph", "윗니를 아랫입술에", stage, emptyList())
        observer.cards.value = listOf(
            CardState(SoundId("sound.f"), 1.0, today - 2, today - 5, 1, 0),
            CardState(one, 1.0, today - 1, today - 5, 1, 0),
            CardState(two, 1.0, today - 1, today - 5, 1, 0)
        )
        val viewModel = viewModel()
        assertThat(viewModel.choice.prompt).isEqualTo(ChoicePromptUi.Meaning("one", "one", speakable = true))

        viewModel.pick("1, 하나")
        viewModel.onAction(ReviewAction.OnContinueClick)
        viewModel.pick("2, 둘")
        viewModel.onAction(ReviewAction.OnContinueClick)

        val result = (viewModel.state.value.phase as ReviewPhase.Finished).result
        assertThat(result.answered).isEqualTo(2)
        assertThat(result.remaining).isEqualTo(0)
    }

    @Test
    fun close_leavesWithoutAsking() = runTest {
        due(one, two)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ReviewAction.OnCloseClick)

            assertThat(awaitItem()).isEqualTo(ReviewEvent.Close)
        }
    }
}
