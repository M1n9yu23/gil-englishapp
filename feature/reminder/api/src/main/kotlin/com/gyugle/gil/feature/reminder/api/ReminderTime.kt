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
package com.gyugle.gil.feature.reminder.api

/** A local time of day for a study reminder, the same every day and in every time zone. */
data class ReminderTime(val hour: Int, val minute: Int) : Comparable<ReminderTime> {
    init {
        require(hour in 0..23) { "Hour out of range: $hour" }
        require(minute in 0..59) { "Minute out of range: $minute" }
    }

    override fun compareTo(other: ReminderTime): Int = compareValuesBy(this, other, { it.hour }, { it.minute })

    companion object {
        /** A learner gets at most this many reminders a day, such as one on the way to work and one back. */
        const val MAX_PER_DAY = 2
    }
}
