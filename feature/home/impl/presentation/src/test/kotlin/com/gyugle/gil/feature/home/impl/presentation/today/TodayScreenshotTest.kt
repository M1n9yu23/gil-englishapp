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
package com.gyugle.gil.feature.home.impl.presentation.today

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.home.impl.presentation.today.TodayPreviewData as Data
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class TodayScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: TodayState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { GilTheme { Surface { TodayScreen(state = state, onAction = {}, onOpenBlock = {}) } } }
        // Past the progress bar's animation, the same instant in every image.
        composeRule.mainClock.advanceTimeBy(SETTLED_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun loading_showsPlaceholders() = capture(TodayState.Loading)

    @Test fun failed_offersRetry() = capture(TodayState.Failed)

    @Test fun firstDay_leadsToTheFirstLesson() = capture(Data.firstDay)

    @Test fun reviewsDue_reviewThenLesson() = capture(Data.reviewsDue)

    @Test fun reviewHeavy_putsReviewsFirst() = capture(Data.reviewHeavy)

    @Test fun goalMet_saysSoAndKeepsGoing() = capture(Data.goalMet)

    @Test fun courseDone_noButton() = capture(Data.courseDone)

    @Test
    @Config(qualifiers = "+night")
    fun reviewHeavy_night() = capture(Data.reviewHeavy)

    @Test
    @Config(qualifiers = "+night")
    fun goalMet_night() = capture(Data.goalMet)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun reviewHeavy_englishFontScale2() = capture(Data.reviewHeavy)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun firstDay_englishFontScale2() = capture(Data.firstDay)

    private companion object {
        const val SETTLED_MILLIS = 400L
    }
}
