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

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.listening.impl.presentation.player.ListeningPreviewData as Data
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class ListeningScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: ListeningState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            GilTheme { Surface { ListeningScreen(state = state, onAction = {}, onClose = {}) } }
        }
        // Past the progress bar's animation, the same instant in every image.
        composeRule.mainClock.advanceTimeBy(SETTLED_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun choosing_listsSourcesUnitsAndPace() = capture(Data.choosing)

    @Test fun choosing_noKoreanVoice_saysSo() = capture(Data.choosingNoKorean)

    @Test fun choosing_pauseDialog() = capture(Data.choosing.copy(dialog = PaceDialog.PAUSE))

    @Test fun preparing_showsProgress() = capture(Data.preparing)

    @Test fun playing_item() = capture(Data.playing)

    @Test fun paused_saysSo() = capture(Data.paused)

    @Test fun playing_dialogueLine() = capture(Data.dialogue)

    @Test fun ended_offersToPlayAgain() = capture(Data.ended)

    @Test fun nothingYet_pointsToLessons() = capture(ListeningState.NothingYet)

    @Test fun noEnglishVoice_offersToInstall() = capture(ListeningState.NoEnglishVoice)

    @Test fun failed_offersRetry() = capture(ListeningState.Failed)

    @Test
    @Config(qualifiers = "+night")
    fun choosing_night() = capture(Data.choosing)

    @Test
    @Config(qualifiers = "+night")
    fun playing_night() = capture(Data.paused)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun choosing_englishFontScale2() = capture(Data.choosingNoKorean)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun playing_englishFontScale2() = capture(Data.dialogue)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun ended_englishFontScale2() = capture(Data.ended)

    @Test
    @Config(qualifiers = "ko-w320dp-h640dp-xhdpi", fontScale = 2f)
    fun playing_narrowFontScale2() = capture(Data.paused)

    private companion object {
        const val SETTLED_MILLIS = 400L
    }
}
