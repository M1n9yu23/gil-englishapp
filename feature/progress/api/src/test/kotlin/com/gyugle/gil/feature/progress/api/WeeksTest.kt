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
package com.gyugle.gil.feature.progress.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.jupiter.api.Test

class WeeksTest {
    private fun day(date: String) = StudyDay(LocalDate.parse(date).toEpochDay())

    @Test
    fun weekOf_mondayFirst_runsMondayToSunday() {
        // 2026-11-08 is a Sunday.
        val week = weekOf(day("2026-11-08"), DayOfWeek.MONDAY)

        assertThat(week.first()).isEqualTo(day("2026-11-02"))
        assertThat(week.last()).isEqualTo(day("2026-11-08"))
    }

    @Test
    fun weekOf_sundayFirst_startsOnToday() {
        val week = weekOf(day("2026-11-08"), DayOfWeek.SUNDAY)

        assertThat(week.first()).isEqualTo(day("2026-11-08"))
        assertThat(week.last()).isEqualTo(day("2026-11-14"))
    }

    @Test
    fun dayOfWeek_readsTheCalendarDay() {
        assertThat(dayOfWeek(day("2026-11-09"))).isEqualTo(DayOfWeek.MONDAY)
    }
}
