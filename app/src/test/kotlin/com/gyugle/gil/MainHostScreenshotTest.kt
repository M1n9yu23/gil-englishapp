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
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.home.impl.presentation.today.BlockUi
import com.gyugle.gil.feature.home.impl.presentation.today.DayUi
import com.gyugle.gil.feature.home.impl.presentation.today.LocalizedTextUi
import com.gyugle.gil.feature.home.impl.presentation.today.TodayMessage
import com.gyugle.gil.feature.home.impl.presentation.today.TodayScreen
import com.gyugle.gil.feature.home.impl.presentation.today.TodayState
import com.gyugle.gil.feature.home.impl.presentation.today.TodayUi
import com.gyugle.gil.feature.stats.impl.presentation.stats.StatsScreen
import com.gyugle.gil.feature.stats.impl.presentation.stats.StatsState
import java.time.DayOfWeek
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Past every fade, so the capture shows the settled screen. */
private const val SETTLED_MILLIS = 1_000L

private val firstLesson = BlockUi.Lesson(
    id = LessonId("lesson.s0-01.1"),
    title = LocalizedTextUi("영어 소리와 만나기", "Meet the sounds of English"),
    unitTitle = LocalizedTextUi("길 위에 서기", "Getting started"),
    minutes = 8,
    enoughNewWords = false
)

/** A learner's first day, the Today tab as a new install shows it. */
private val firstDay = TodayState.Ready(
    TodayUi(
        message = TodayMessage.FIRST_DAY,
        studiedMinutes = 0,
        goalMinutes = 20,
        blocks = listOf(firstLesson),
        next = firstLesson,
        firstLesson = true,
        streak = 0,
        week = DayOfWeek.entries.map { DayUi(it, studied = false, isToday = it == DayOfWeek.MONDAY, muted = true) }
    )
)

/** A new install on the Stats tab. */
private val noHistory = StatsState.Empty(firstLesson = firstLesson.id)

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi", application = Application::class)
class MainHostScreenshotTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun capture(@StringRes tab: Int? = null) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            GilTheme {
                MainHost(entries = {
                    testMainEntries(
                        it,
                        today = { TodayScreen(firstDay, onAction = {}, onOpenBlock = {}) },
                        stats = { StatsScreen(noHistory, onAction = {}, onOpenLesson = {}) }
                    )
                })
            }
        }
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

    @Test
    @Config(qualifiers = "ko-w760dp-h360dp-xhdpi")
    fun today_phoneLandscape_showsRail() = capture()

    @Test
    @Config(qualifiers = "ko-w673dp-h841dp-xhdpi")
    fun today_foldable_showsRail() = capture()

    @Test
    @Config(qualifiers = "ko-w1280dp-h800dp-mdpi")
    fun today_tabletLandscape_centersContent() = capture()

    @Test
    @Config(qualifiers = "ko-w1280dp-h800dp-mdpi")
    fun progress_tabletLandscape_centersContent() = capture(R.string.main_tab_progress)
}
