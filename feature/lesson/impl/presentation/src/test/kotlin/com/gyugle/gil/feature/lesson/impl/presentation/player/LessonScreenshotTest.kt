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

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.lesson.impl.presentation.player.LessonPreviewData as Data
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class LessonScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: LessonState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { GilTheme { LessonScreen(state = state, onAction = {}) } }
        // Past the fade and the progress bar's animation, the same instant in every image.
        composeRule.mainClock.advanceTimeBy(SETTLED_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun loading_showsTheCloseMarkAndPlaceholders() = capture(LessonState())

    @Test fun failed_offersRetry() = capture(Data.failed)

    @Test fun askResume_offersContinueOrStartOver() = capture(Data.askResume)

    @Test fun explain_paragraphsAndExamples() = capture(Data.playing(Data.explain, stepNumber = 2))

    @Test fun listen_meaningHidden() = capture(Data.playing(Data.listen, stepNumber = 1))

    @Test fun shadow_asksForTheMicrophone() = capture(Data.playing(Data.shadow))

    @Test fun shadow_recorded_offersPlayback() = capture(Data.playing(Data.shadowRecorded))

    @Test fun shadow_scored_underlinesMissedWords() = capture(Data.playing(Data.shadowScored))

    @Test fun shadow_nothingHeard_asksToTryLouder() = capture(Data.playing(Data.shadowNothingHeard))

    @Test fun shadow_noModel_saysHowToGetScoring() = capture(Data.playing(Data.shadowScoringNotice))

    @Test fun chooseMeaning_beforeAnswering() = capture(Data.playing(Data.chooseMeaning))

    @Test fun chooseMeaning_wrong_explainsAndComesBack() = capture(
        Data.playing(Data.chooseMeaningWrong, Data.wrongFeedback)
    )

    @Test fun listenChoose_right_revealsTheWord() =
        capture(Data.playing(Data.listenChooseAnswered, FeedbackUi(GradeUi.CORRECT, translation = "승합차")))

    @Test fun chooseEnglish_koreanQuestion() = capture(Data.playing(Data.chooseEnglish))

    @Test fun minimalPair_answered_showsWhatWasHeard() = capture(
        Data.playing(Data.minimalPairAnswered, FeedbackUi(GradeUi.CORRECT, why = "들린 단어는 `sheep`(양)이에요."))
    )

    @Test fun dialogue_beforeAnswering() = capture(Data.playing(Data.dialogue))

    @Test fun passage_firstQuestion() = capture(Data.playing(Data.passage))

    @Test fun fillBlank_typed_readyToCheck() = capture(Data.playing(Data.fillBlankTyped, canCheck = true))

    @Test fun arrange_halfBuilt() = capture(Data.playing(Data.arrange, canCheck = true))

    @Test fun spell_close_showsTheSpelling() = capture(
        Data.playing(Data.spell, FeedbackUi(GradeUi.CLOSE, answer = "five"))
    )

    @Test fun dictation_wrong_marksTheWord() = capture(Data.playing(Data.dictation, Data.dictationFeedback))

    @Test fun again_marksTheRetry() =
        capture(Data.playing(Data.chooseMeaning, stepNumber = 10, stepCount = 10, isAgain = true))

    @Test fun noVoice_tellsOnceAndShowsText() = capture(
        Data.playing(
            StepUi.Choice(ChoicePrompt.Audio("van"), Data.listenChooseAnswered.options, null, null),
            stepNumber = 1,
            hasVoice = false,
            showsNoVoiceNotice = true
        )
    )

    @Test fun result_listsTheItems() = capture(Data.result)

    @Test
    fun exitDialog_asksBeforeLeaving() = capture(Data.playing(Data.chooseMeaning).copy(isExitConfirmShown = true))

    @Test
    @Config(qualifiers = "+night")
    fun chooseMeaning_wrong_night() = capture(Data.playing(Data.chooseMeaningWrong, Data.wrongFeedback))

    @Test
    @Config(qualifiers = "+night")
    fun explain_night() = capture(Data.playing(Data.explain, stepNumber = 2))

    @Test
    @Config(qualifiers = "+night")
    fun result_night() = capture(Data.result)

    @Test
    @Config(qualifiers = "+night")
    fun shadow_scored_night() = capture(Data.playing(Data.shadowScored))

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun chooseMeaning_wrong_englishFontScale2() = capture(Data.playing(Data.chooseMeaningWrong, Data.wrongFeedback))

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun arrange_englishFontScale2() = capture(Data.playing(Data.arrange, canCheck = true))

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun result_englishFontScale2() = capture(Data.result)

    private companion object {
        const val SETTLED_MILLIS = 400L
    }
}
