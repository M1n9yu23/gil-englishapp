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
package com.gyugle.gil.feature.stats.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.StudyDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.jupiter.api.Test

class CalendarTest {
    private fun day(date: String) = StudyDay(LocalDate.parse(date).toEpochDay())

    @Test
    fun monthOf_lastDayOfTheMonth_staysInIt() {
        assertThat(monthOf(day("2026-11-30"))).isEqualTo(YearMonth.of(2026, 11))
    }

    @Test
    fun calendarWeeks_mondayFirst_padsBeforeTheFirstAndAfterTheLast() {
        // 2026-11-01 is a Sunday and 2026-11-30 a Monday.
        val weeks = calendarWeeks(YearMonth.of(2026, 11), DayOfWeek.MONDAY)

        assertThat(weeks.size).isEqualTo(6)
        assertThat(weeks.first()).isEqualTo(List(6) { null } + day("2026-11-01"))
        assertThat(weeks.last()).isEqualTo(listOf(day("2026-11-30")) + List(6) { null })
    }

    @Test
    fun calendarWeeks_sundayFirst_startsOnTheFirst() {
        val weeks = calendarWeeks(YearMonth.of(2026, 11), DayOfWeek.SUNDAY)

        assertThat(weeks.size).isEqualTo(5)
        assertThat(weeks.first().first()).isEqualTo(day("2026-11-01"))
        assertThat(weeks.last()).isEqualTo(listOf(day("2026-11-29"), day("2026-11-30")) + List(5) { null })
    }

    @Test
    fun calendarWeeks_monthFillsWholeWeeks_hasNoPadding() {
        // 2027-02-01 is a Monday and February 2027 has 28 days.
        val weeks = calendarWeeks(YearMonth.of(2027, 2), DayOfWeek.MONDAY)

        assertThat(weeks.size).isEqualTo(4)
        assertThat(weeks.flatten().none { it == null }).isEqualTo(true)
    }

    @Test
    fun totalTime_daysOutsideTheRange_areLeftOut() {
        val studyTime = listOf(
            DailyStudyTime(day("2026-10-31"), 30.minutes),
            DailyStudyTime(day("2026-11-01"), 12.minutes),
            DailyStudyTime(day("2026-11-30"), 8.minutes),
            DailyStudyTime(day("2026-12-01"), 5.minutes)
        )

        val month = YearMonth.of(2026, 11)
        assertThat(totalTime(studyTime, month.firstDay(), month.lastDay())).isEqualTo(20.minutes)
    }

    @Test
    fun totalTime_noStudy_isZero() {
        assertThat(totalTime(emptyList(), day("2026-11-01"), day("2026-11-07"))).isEqualTo(Duration.ZERO)
    }
}
