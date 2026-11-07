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
package com.gyugle.gil.feature.dictionary.impl.presentation.entry

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
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
class EntryScreenScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private val voices = VoicesUi(american = true, british = true)

    private fun capture(state: EntryState, report: Boolean = true) = composeRule.captureScreen {
        EntryScreen(
            state = state,
            onAction = {},
            navigation = EntryNavigation({}, {}, { _, _ -> }),
            onReport = if (report) ({}) else null
        )
    }

    private fun loaded(entry: EntryUi, voices: VoicesUi = this.voices) =
        EntryState(isLoading = false, entry = entry, voices = voices)

    @Test fun word_light_showsMeaningsToKnowNow() = capture(loaded(EntryPreviewData.word))

    @Test
    @Config(qualifiers = "+night")
    fun word_night_usesNightPaper() = capture(loaded(EntryPreviewData.word))

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun word_englishFontScale2_wrapsAccentButtons() = capture(loaded(EntryPreviewData.word))

    /** The whole page with the later meanings opened. */
    @Test
    @Config(qualifiers = "ko-w360dp-h1800dp-xhdpi")
    fun word_tall_showsEverySectionWithLaterMeaningsOpen() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            GilTheme {
                Surface {
                    EntryScreen(loaded(EntryPreviewData.word), {}, EntryNavigation({}, {}, { _, _ -> }), onReport = {})
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithText("나중에 배울 뜻 2개").performClick()
        composeRule.mainClock.advanceTimeBy(600)
        composeRule.onRoot().captureRoboImage()
    }

    @Test
    fun newWord_light_showsFirstMeaningsWithoutVoices() =
        capture(loaded(EntryPreviewData.newWord, voices = VoicesUi()), report = false)

    @Test fun pattern_light_showsMeaningAndNote() = capture(loaded(EntryPreviewData.pattern))

    @Test fun sound_light_showsSpellingAndExampleWords() = capture(loaded(EntryPreviewData.sound))

    @Test
    @Config(qualifiers = "+night")
    fun sound_night_showsSpellingAndExampleWords() = capture(loaded(EntryPreviewData.sound))

    @Test fun loading_light_showsPlaceholders() = capture(EntryState())

    @Test
    fun notFound_light_offersTheWayBack() =
        capture(EntryState(isLoading = false, error = ContentError.NOT_FOUND.toContentErrorUi()))
}
