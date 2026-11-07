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
package com.gyugle.gil.feature.reminder.impl.domain

import com.gyugle.gil.feature.reminder.api.ReminderTime
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * The first moment after [now] when the clock in [zone] shows [time].
 *
 * A time that a daylight saving change skips is moved forward by the length of the gap, and a
 * time that happens twice uses the first one.
 */
fun nextReminderAt(time: ReminderTime, now: Instant, zone: TimeZone): Instant {
    val localTime = LocalTime(time.hour, time.minute)
    val today = now.toLocalDateTime(zone).date
    val todayAt = LocalDateTime(today, localTime).toInstant(zone)
    if (todayAt > now) return todayAt
    return LocalDateTime(today.plus(1, DateTimeUnit.DAY), localTime).toInstant(zone)
}

/**
 * The times to save: each one once, in the order of the day.
 *
 * @throws IllegalArgumentException for more than [ReminderTime.MAX_PER_DAY] different times.
 */
fun reminderTimesToSave(times: List<ReminderTime>): List<ReminderTime> {
    val distinct = times.distinct().sorted()
    require(distinct.size <= ReminderTime.MAX_PER_DAY) {
        "At most ${ReminderTime.MAX_PER_DAY} reminder times, got ${distinct.size}"
    }
    return distinct
}
