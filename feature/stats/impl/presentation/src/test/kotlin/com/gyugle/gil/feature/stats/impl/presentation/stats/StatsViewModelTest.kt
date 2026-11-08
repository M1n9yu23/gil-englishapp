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

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageLessons
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import java.time.LocalDate
import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** The fake clock's today is Saturday 2026-11-07. */
class StatsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val s0 = StageTag(StageId("stage.s0"), 0, LocalizedText("소리", "Sounds"))
    private val a1 = StageTag(StageId("stage.a1"), 1, LocalizedText("첫걸음", "First steps"))
    private val course = FakeCourseObserver().apply {
        stages = listOf(
            StageLessons(s0, listOf(LessonId("lesson.s0-01.1"), LessonId("lesson.s0-01.2"))),
            StageLessons(a1, emptyList())
        )
    }
    private val progress = FakeProgressObserver()
    private val clock = FakeStudyClock()
    private val savedState = SavedStateHandle()
    private val defaultLocale = Locale.getDefault()

    // The week starts on Sunday in this locale, whatever the machine running the tests uses.
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        Locale.setDefault(Locale.KOREA)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        Locale.setDefault(defaultLocale)
    }

    private fun viewModel() = StatsViewModel(progress, course, clock, savedState)

    private fun day(date: String) = StudyDay(LocalDate.parse(date).toEpochDay())

    private fun card(stabilityDays: Double, due: String) = CardState(
        SenseId("sense.$due.$stabilityDays"),
        stabilityDays = stabilityDays,
        dueDay = day(due),
        lastReviewDay = day("2026-11-01"),
        reviewCount = 2,
        lapseCount = 0
    )

    /** The first state past loading. */
    private suspend fun ReceiveTurbine<StatsState>.settled(): StatsState {
        var state = awaitItem()
        while (state is StatsState.Loading) state = awaitItem()
        return state
    }

    /** The first figures that pass [check]. The record's flows each emit on a change. */
    private suspend fun ReceiveTurbine<StatsState>.readyWhen(check: (StatsUi) -> Boolean = { true }): StatsUi {
        var state = settled()
        while (state !is StatsState.Ready || !check(state.stats)) state = settled()
        return state.stats
    }

    private fun studied() {
        progress.studyTime.value = listOf(
            DailyStudyTime(day("2026-10-20"), 30.minutes),
            DailyStudyTime(day("2026-11-03"), 12.minutes),
            DailyStudyTime(day("2026-11-05"), 3.minutes),
            DailyStudyTime(day("2026-11-07"), 75.minutes)
        )
        progress.cards.value = listOf(
            card(2.0, due = "2026-11-02"),
            card(5.0, due = "2026-11-07"),
            card(10.0, due = "2026-11-09"),
            card(30.0, due = "2026-12-01")
        )
        progress.completedLessons.value = setOf(LessonId("lesson.s0-01.1"))
        progress.streak.value = 4
    }

    @Test
    fun state_noRecord_leadsToTheFirstLesson() = runTest(dispatcher) {
        viewModel().state.test {
            assertThat(settled()).isEqualTo(StatsState.Empty(LessonId("lesson.s0-01.1")))
        }
    }

    @Test
    fun state_firstLessonLeftHalfway_staysEmpty() = runTest(dispatcher) {
        progress.studyTime.value = listOf(DailyStudyTime(day("2026-11-07"), 2.minutes))

        viewModel().state.test {
            assertThat(settled()).isEqualTo(StatsState.Empty(LessonId("lesson.s0-01.1")))
        }
    }

    @Test
    fun state_record_showsTimeStreakReviewsLevelsAndStages() = runTest(dispatcher) {
        studied()

        viewModel().state.test {
            val stats = readyWhen { it.streakDays == 4 && it.stages.single().completed == 1 }

            // The week of Saturday 11-07 runs from Sunday 11-01.
            assertThat(stats.weekMinutes).isEqualTo(90)
            assertThat(stats.monthMinutes).isEqualTo(90)
            assertThat(stats.forecast.map { it.count }).isEqualTo(listOf(2, 0, 1, 0, 0, 0, 0))
            assertThat(stats.forecast.first().isToday).isEqualTo(true)
            assertThat(stats.levels).isEqualTo(LevelsUi(learning = 2, familiar = 1, lasting = 1))
            assertThat(stats.stages).isEqualTo(listOf(StageUi(LocalizedTextUi("소리", "Sounds"), 1, 2)))
        }
    }

    @Test
    fun state_record_marksOnlyStudyDaysOfThisMonth() = runTest(dispatcher) {
        studied()

        viewModel().state.test {
            val calendar = readyWhen { it.calendar.studiedDays > 0 }.calendar
            val days = calendar.weeks.flatten().filterNotNull()

            assertThat(calendar.year to calendar.month).isEqualTo(2026 to 11)
            // 11-05 has under five minutes, so it is not a study day.
            assertThat(days.filter { it.studied }.map { it.dayOfMonth }).isEqualTo(listOf(3, 7))
            assertThat(days.single { it.isToday }.dayOfMonth).isEqualTo(7)
            assertThat(days.first { it.ahead }.dayOfMonth).isEqualTo(8)
            assertThat(calendar.hasPrevious).isEqualTo(true)
            assertThat(calendar.hasNext).isEqualTo(false)
        }
    }

    @Test
    fun onPreviousMonthClick_movesBackAsFarAsTheFirstRecord() = runTest(dispatcher) {
        studied()
        val viewModel = viewModel()

        viewModel.state.test {
            readyWhen { it.calendar.studiedDays > 0 }
            viewModel.onAction(StatsAction.OnPreviousMonthClick)
            val october = readyWhen { it.calendar.month == 10 }.calendar

            assertThat(october.studiedDays).isEqualTo(1)
            assertThat(october.hasPrevious).isEqualTo(false)
            assertThat(october.hasNext).isEqualTo(true)

            viewModel.onAction(StatsAction.OnPreviousMonthClick)
            viewModel.onAction(StatsAction.OnNextMonthClick)
            assertThat(readyWhen().calendar.month).isEqualTo(11)
        }
    }

    @Test
    fun onNextMonthClick_backOnThisMonth_followsTheMonthWhenItEnds() = runTest(dispatcher) {
        studied()
        val viewModel = viewModel()

        viewModel.state.test {
            readyWhen { it.calendar.studiedDays > 0 }
            viewModel.onAction(StatsAction.OnPreviousMonthClick)
            readyWhen { it.calendar.month == 10 }
            viewModel.onAction(StatsAction.OnNextMonthClick)
            readyWhen { it.calendar.month == 11 }

            // Saturday 11-07 to Tuesday 12-01.
            clock.advanceBy(24.days)
            advanceTimeBy(2.minutes)

            assertThat(readyWhen { it.calendar.month == 12 }.calendar.hasNext).isEqualTo(false)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_savedMonthBeforeTheFirstRecord_showsTheFirstMonth() = runTest(dispatcher) {
        studied()
        savedState["shownMonth"] = "2026-01"

        viewModel().state.test {
            assertThat(readyWhen().calendar.month).isEqualTo(10)
        }
    }

    @Test
    fun state_savedMonthAfterThisMonth_showsThisMonth() = runTest(dispatcher) {
        studied()
        savedState["shownMonth"] = "2027-03"

        viewModel().state.test {
            assertThat(readyWhen().calendar.month).isEqualTo(11)
        }
    }

    @Test
    fun state_savedMonthUnreadable_showsThisMonth() = runTest(dispatcher) {
        studied()
        savedState["shownMonth"] = "November"

        viewModel().state.test {
            assertThat(readyWhen().calendar.month).isEqualTo(11)
        }
    }

    @Test
    fun state_noRecordAndNoLessons_hasNoFirstLesson() = runTest(dispatcher) {
        course.stages = listOf(StageLessons(a1, emptyList()))

        viewModel().state.test {
            assertThat(settled()).isEqualTo(StatsState.Empty(firstLesson = null))
        }
    }

    @Test
    fun state_monthSavedBeforeProcessDeath_showsThatMonth() = runTest(dispatcher) {
        studied()
        savedState["shownMonth"] = "2026-10"

        viewModel().state.test {
            assertThat(readyWhen().calendar.month).isEqualTo(10)
        }
    }

    @Test
    fun state_newDay_movesTodayAndTheForecast() = runTest(dispatcher) {
        studied()

        viewModel().state.test {
            readyWhen { it.forecast.first().count == 2 }
            clock.advanceBy(1.days)
            advanceTimeBy(2.minutes)

            val stats =
                readyWhen { it.calendar.weeks.flatten().any { day -> day?.isToday == true && day.dayOfMonth == 8 } }
            assertThat(stats.forecast.map { it.count }).isEqualTo(listOf(2, 1, 0, 0, 0, 0, 0))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun state_courseUnreadable_failsAndRetryRecovers() = runTest(dispatcher) {
        course.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()

        viewModel.state.test {
            assertThat(settled()).isEqualTo(StatsState.Failed)

            course.error = null
            viewModel.onAction(StatsAction.OnRetryClick)

            assertThat(settled()).isInstanceOf(StatsState.Empty::class)
        }
    }
}
