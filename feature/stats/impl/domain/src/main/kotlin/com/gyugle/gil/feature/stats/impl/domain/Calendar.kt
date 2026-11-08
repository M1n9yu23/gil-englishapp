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

import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.StudyDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.time.Duration

private const val DAYS_IN_WEEK = 7

/** The calendar month [day] falls in. */
fun monthOf(day: StudyDay): YearMonth = YearMonth.from(LocalDate.ofEpochDay(day.epochDay))

fun YearMonth.firstDay(): StudyDay = StudyDay(atDay(1).toEpochDay())

fun YearMonth.lastDay(): StudyDay = StudyDay(atEndOfMonth().toEpochDay())

/**
 * The days of [month] as calendar rows of seven from [firstDayOfWeek]. Cells before the first and
 * after the last day of the month are null.
 */
fun calendarWeeks(month: YearMonth, firstDayOfWeek: DayOfWeek): List<List<StudyDay?>> {
    val first = month.firstDay()
    val lead = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val cells = List(lead) { null } + List(month.lengthOfMonth()) { first + it }
    val trail = (DAYS_IN_WEEK - cells.size % DAYS_IN_WEEK) % DAYS_IN_WEEK
    return (cells + List(trail) { null }).chunked(DAYS_IN_WEEK)
}

/** The study time of the days from [from] to [to], both included. */
fun totalTime(studyTime: List<DailyStudyTime>, from: StudyDay, to: StudyDay): Duration = studyTime
    .filter { it.day in from..to }
    .fold(Duration.ZERO) { total, day -> total + day.duration }
