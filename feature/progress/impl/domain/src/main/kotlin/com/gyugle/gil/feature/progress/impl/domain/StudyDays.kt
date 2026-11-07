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
package com.gyugle.gil.feature.progress.impl.domain

import com.gyugle.gil.feature.progress.api.StudyDay
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** A day counts toward the streak with at least this much study. */
val MIN_STUDY_TIME_PER_DAY = 5.minutes

/** The local time a new study day begins. Late-night study belongs to the evening before. */
private val DAY_START = LocalTime(hour = 4, minute = 0)

private const val STREAK_WINDOW_DAYS = 7
private const val MAX_DAYS_OFF_IN_WINDOW = 2

/** The study day [instant] falls on in [zone]. */
fun studyDayOf(instant: Instant, zone: TimeZone): StudyDay {
    val local = instant.toLocalDateTime(zone)
    val date = if (local.time < DAY_START) local.date.minus(1, DateTimeUnit.DAY) else local.date
    return StudyDay(date.toEpochDays())
}

/**
 * Counts the study days in the streak that reaches [today].
 *
 * Walking back from [today], the streak ends at the day off that makes three days off within
 * seven days in a row. [today] is not a day off yet: the learner may still study.
 *
 * @param studyDays days with at least [MIN_STUDY_TIME_PER_DAY] of study.
 */
fun studyStreak(studyDays: Set<StudyDay>, today: StudyDay): Int {
    val earliest = studyDays.minOrNull() ?: return 0
    // Days off seen so far, latest first; only those in the seven days from the current one matter.
    val daysOff = ArrayDeque<StudyDay>()
    var streak = 0
    var day = today
    while (day >= earliest) {
        if (day in studyDays) {
            streak++
        } else if (day != today) {
            daysOff.addLast(day)
            while (day.daysUntil(daysOff.first()) >= STREAK_WINDOW_DAYS) daysOff.removeFirst()
            if (daysOff.size > MAX_DAYS_OFF_IN_WINDOW) break
        }
        day -= 1
    }
    return streak
}
