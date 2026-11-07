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
package com.gyugle.gil

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Past every fade, so the capture shows the settled screen. */
private const val SETTLED_MILLIS = 1_000L

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi", application = Application::class)
class MainHostScreenshotTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(@StringRes tab: Int? = null) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { GilTheme { MainHost(entries = { testMainEntries(it) }) } }
        if (tab != null) {
            val label = composeRule.activity.getString(tab)
            composeRule.onNode(hasText(label) and isSelectable()).performClick()
        }
        composeRule.mainClock.advanceTimeBy(SETTLED_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun today_light_showsFirstStudyHint() = capture()

    @Test
    @Config(qualifiers = "+night")
    fun today_night_showsFirstStudyHint() = capture()

    @Test
    @Config(fontScale = 2f)
    fun today_fontScale2_keepsBarAndTextWhole() = capture()

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi")
    fun today_english_showsEnglishLabels() = capture()

    // "Stats" and "Words" are the widest bar labels.
    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun today_englishFontScale2_keepsBarLabelsWhole() = capture()

    @Test
    @Config(qualifiers = "en-w320dp-h640dp-xhdpi", fontScale = 2f)
    fun today_narrowEnglishFontScale2_keepsBarLabelsWhole() = capture()

    @Test fun progress_light_showsNoHistory() = capture(R.string.main_tab_progress)

    @Test
    @Config(qualifiers = "+night")
    fun progress_night_showsNoHistory() = capture(R.string.main_tab_progress)
}
