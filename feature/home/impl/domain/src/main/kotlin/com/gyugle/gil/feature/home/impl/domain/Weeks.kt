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
package com.gyugle.gil.feature.home.impl.domain

import com.gyugle.gil.feature.progress.api.StudyDay
import java.time.DayOfWeek
import java.time.LocalDate

private const val DAYS_IN_WEEK = 7

/** The seven days of the week [today] is in, from [firstDayOfWeek], which depends on the locale. */
fun weekOf(today: StudyDay, firstDayOfWeek: DayOfWeek): List<StudyDay> {
    val weekday = LocalDate.ofEpochDay(today.epochDay).dayOfWeek
    val sinceStart = (weekday.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val start = today - sinceStart
    return List(DAYS_IN_WEEK) { start + it }
}

/** The day of the week [day] falls on. */
fun dayOfWeek(day: StudyDay): DayOfWeek = LocalDate.ofEpochDay(day.epochDay).dayOfWeek
