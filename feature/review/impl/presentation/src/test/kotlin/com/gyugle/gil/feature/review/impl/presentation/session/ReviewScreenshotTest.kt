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

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.review.impl.presentation.session.ReviewPreviewData as Data
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class ReviewScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: ReviewState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { GilTheme { ReviewScreen(state = state, onAction = {}) } }
        // Past the fade and the progress bar's animation, the same instant in every image.
        composeRule.mainClock.advanceTimeBy(SETTLED_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun loading_showsTheCloseMarkAndPlaceholders() = capture(ReviewState())

    @Test fun failed_offersRetry() = capture(Data.failed)

    @Test fun empty_saysWhenTheNextReviewIs() = capture(Data.empty)

    @Test fun empty_noCards_pointsToLessons() = capture(Data.emptyNoCards)

    @Test fun meaning_beforeAnswering() = capture(Data.meaningIdle)

    @Test fun meaning_wrong_showsTheAnswerAndComesBack() = capture(Data.meaningAnswered)

    @Test fun english_koreanPrompt() = capture(Data.englishIdle)

    @Test fun listen_right_revealsTheWordAndMeaning() = capture(Data.listenAnswered)

    @Test fun letter_noVoice_readsTheName() = capture(Data.letterNoVoice)

    @Test fun letter_byEar_hidesTheName() = capture(
        ReviewState(
            (Data.letterNoVoice.phase as ReviewPhase.Asking).copy(
                kind = CardKindUi.CHOOSE_LETTER_BY_EAR,
                card = CardUi.Choice(
                    prompt = ChoicePromptUi.Letter(nameIpa = "dʒiː", showsIpa = false),
                    options = listOf("G g", "J j", "Z z", "E e").map { AnswerUi(it, english = true) }
                ),
                hasVoice = true
            )
        )
    )

    @Test fun sound_wrong_showsIpaAndTheDescription() = capture(Data.soundAnswered)

    @Test fun spell_typed_readyToCheck() = capture(Data.spellTyping)

    @Test fun spell_byMeaning_hasNoSpeaker() = capture(
        ReviewState(
            (Data.spellTyping.phase as ReviewPhase.Asking).copy(
                kind = CardKindUi.SPELL_BY_MEANING,
                card = CardUi.Typed(TypedPromptUi.Spell("수요일", "ˈwɛnzdeɪ", speakable = false), text = "")
            )
        )
    )

    @Test fun blank_close_showsTheSpelling() = capture(Data.blankClose)

    @Test fun result_moreDue_offersToKeepGoing() = capture(Data.finished)

    @Test fun result_allDone_saysWhenTheNextIs() = capture(Data.finishedAllDone)

    @Test
    @Config(qualifiers = "+night")
    fun meaning_wrong_night() = capture(Data.meaningAnswered)

    @Test
    @Config(qualifiers = "+night")
    fun result_night() = capture(Data.finished)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun sound_wrong_englishFontScale2() = capture(Data.soundAnswered)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun blank_close_englishFontScale2() = capture(Data.blankClose)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun result_englishFontScale2() = capture(Data.finished)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun empty_englishFontScale2() = capture(Data.empty)

    @Test fun test_intro_saysWhatPassingDoes() = capture(Data.testIntro)

    @Test fun test_wrong_doesNotSayItComesBack() = capture(Data.testWrong)

    @Test fun test_passed_saysWhatWasRecorded() = capture(Data.testPassed)

    @Test fun test_failedEarly_saysWhyItStopped() = capture(Data.testFailedEarly)

    @Test fun test_failedAtTheEnd_answeredEveryQuestion() {
        val passed = (Data.testPassed.phase as ReviewPhase.TestFinished).result
        capture(Data.testPassed.copy(phase = ReviewPhase.TestFinished(passed.copy(passed = false, right = 14))))
    }

    @Test fun test_failed_namesTheTest() = capture(Data.testFailed)

    @Test
    @Config(qualifiers = "+night")
    fun test_intro_night() = capture(Data.testIntro)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun test_intro_englishFontScale2() = capture(Data.testIntro)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun test_passed_englishFontScale2() = capture(Data.testPassed)

    private companion object {
        const val SETTLED_MILLIS = 400L
    }
}
