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
package com.gyugle.gil.feature.dictionary.impl.presentation.sentence

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.dictionary.impl.presentation.captureScreen
import com.gyugle.gil.feature.dictionary.impl.presentation.toContentErrorUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class SentenceSheetScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: SentenceState) = composeRule.captureScreen(sheet = true) {
        SentenceSheet(state = state, onAction = {}, onBack = {}, onOpenEntry = {}, onReport = {})
    }

    private fun loaded(sentence: SentenceUi, selected: Int? = null) =
        SentenceState(isLoading = false, sentence = sentence, selected = selected, hasVoice = true)

    @Test fun breakdown_light_showsTheTappedWordAndChunks() = capture(
        loaded(SentencePreviewData.sentence, selected = 1)
    )

    @Test
    @Config(qualifiers = "+night")
    fun breakdown_night_showsTheTappedWordAndChunks() = capture(loaded(SentencePreviewData.sentence, selected = 1))

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun breakdown_englishFontScale2_wrapsTheSentence() = capture(loaded(SentencePreviewData.sentence, selected = 1))

    @Test
    fun wordWithoutSense_light_pointsToTheWordPage() = capture(loaded(SentencePreviewData.sentence, selected = 3))

    @Test fun plain_light_showsTheTapHint() = capture(loaded(SentencePreviewData.plain))

    @Test fun loading_light_showsTwoLines() = capture(SentenceState())

    @Test
    fun notFound_light_offersTheWayBack() =
        capture(SentenceState(isLoading = false, error = ContentError.NOT_FOUND.toContentErrorUi()))

    @Test
    fun unavailable_light_offersToTryAgain() =
        capture(SentenceState(isLoading = false, error = ContentError.UNAVAILABLE.toContentErrorUi()))
}
