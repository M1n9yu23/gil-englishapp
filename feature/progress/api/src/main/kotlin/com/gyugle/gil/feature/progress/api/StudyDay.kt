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

/**
 * One day of study. It starts at 04:00 local time, so a lesson at 1 a.m. still counts toward the
 * evening before.
 *
 * Counted in days from 1970-01-01 like `LocalDate.toEpochDays()`, so a calendar can show it. A
 * record keeps the day it was made on; changing the time zone later does not move old records.
 */
@JvmInline
value class StudyDay(val epochDay: Long) : Comparable<StudyDay> {
    operator fun plus(days: Int): StudyDay = StudyDay(epochDay + days)

    operator fun minus(days: Int): StudyDay = StudyDay(epochDay - days)

    /** Whole days from this day to [other], negative when [other] is earlier. */
    fun daysUntil(other: StudyDay): Long = other.epochDay - epochDay

    override fun compareTo(other: StudyDay): Int = epochDay.compareTo(other.epochDay)
}
