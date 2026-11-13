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
package com.gyugle.gil.feature.stats.impl.presentation.stats

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.stats.impl.presentation.stats.StatsPreviewData as Data
import java.time.DayOfWeek
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The tall captures show the whole page, which a phone scrolls through. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class StatsScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: StatsState) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { GilTheme { Surface { StatsScreen(state = state, onAction = {}, onOpenLesson = {}) } } }
        composeRule.mainClock.advanceTimeBy(SETTLED_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    @Test fun loading_showsPlaceholders() = capture(StatsState.Loading)

    @Test fun failed_offersRetry() = capture(StatsState.Failed)

    @Test fun empty_leadsToTheFirstLesson() = capture(Data.empty)

    @Test fun ready_showsTimeAndCalendarFirst() = capture(Data.ready)

    @Test
    @Config(qualifiers = "ko-w360dp-h1500dp-xhdpi")
    fun ready_wholePage() = capture(Data.ready)

    @Test
    @Config(qualifiers = "ko-w360dp-h1500dp-xhdpi")
    fun firstWeek_wholePage() = capture(Data.firstWeek)

    @Test
    @Config(qualifiers = "ko-w360dp-h1500dp-night-xhdpi")
    fun ready_wholePageNight() = capture(Data.ready)

    @Test
    @Config(qualifiers = "en-w360dp-h1500dp-xhdpi")
    fun ready_wholePageEnglish() = capture(Data.ready)

    @Test
    @Config(qualifiers = "ko-w360dp-h3000dp-xhdpi", fontScale = 2f)
    fun ready_wholePageFontScale2() = capture(Data.ready)

    @Test
    @Config(qualifiers = "en-w320dp-h3000dp-xhdpi", fontScale = 2f)
    fun ready_narrowEnglishFontScale2() = capture(Data.ready)

    @Test
    @Config(qualifiers = "+night")
    fun empty_night() = capture(Data.empty)

    /** The calendar alone, where seven columns are tightest. */
    @Test
    @Config(qualifiers = "en-w320dp-h640dp-xhdpi", fontScale = 2f)
    fun calendar_narrowEnglishFontScale2_keepsDaysApart() {
        composeRule.setContent {
            GilTheme {
                Surface {
                    StudyCalendar(Data.ready.stats.calendar, onPrevious = {}, onNext = {})
                }
            }
        }
        composeRule.onRoot().captureRoboImage()
    }

    /** A month gone by: no today, nothing ahead, and the way forward open. */
    @Test
    fun calendar_pastMonth_canGoForward() {
        val studied = setOf(5, 6, 7, 12, 20, 21, 30)
        val october = CalendarUi(
            year = 2026,
            month = 10,
            weekdays = List(7) { DayOfWeek.MONDAY.plus(it.toLong()) },
            weeks = (List(3) { null } + (1..31).toList() + List(1) { null }).chunked(7).map { week ->
                week.map { date ->
                    date?.let { CalendarDayUi(it, studied = it in studied, isToday = false, ahead = false) }
                }
            },
            studiedDays = studied.size,
            hasPrevious = false,
            hasNext = true
        )
        composeRule.setContent { GilTheme { Surface { StudyCalendar(october, onPrevious = {}, onNext = {}) } } }
        composeRule.onRoot().captureRoboImage()
    }

    private companion object {
        const val SETTLED_MILLIS = 400L
    }
}
