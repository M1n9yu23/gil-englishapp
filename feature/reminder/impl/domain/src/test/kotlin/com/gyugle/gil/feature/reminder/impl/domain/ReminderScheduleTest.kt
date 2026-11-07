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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.feature.reminder.api.ReminderTime
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Test

class ReminderScheduleTest {

    private val seoul = TimeZone.of("Asia/Seoul")
    private val newYork = TimeZone.of("America/New_York")

    @Test
    fun nextReminderAt_laterToday_isToday() {
        val now = Instant.parse("2026-11-07T07:00:00+09:00")

        assertThat(nextReminderAt(ReminderTime(20, 30), now, seoul))
            .isEqualTo(Instant.parse("2026-11-07T20:30:00+09:00"))
    }

    @Test
    fun nextReminderAt_alreadyPassedToday_isTomorrow() {
        val now = Instant.parse("2026-11-07T21:00:00+09:00")

        assertThat(nextReminderAt(ReminderTime(20, 30), now, seoul))
            .isEqualTo(Instant.parse("2026-11-08T20:30:00+09:00"))
    }

    // The alarm fires at the reminder time itself; the next one must be a day later, not again now.
    @Test
    fun nextReminderAt_exactlyNow_isTomorrow() {
        val now = Instant.parse("2026-11-07T20:30:00+09:00")

        assertThat(nextReminderAt(ReminderTime(20, 30), now, seoul))
            .isEqualTo(Instant.parse("2026-11-08T20:30:00+09:00"))
    }

    @Test
    fun nextReminderAt_lastMonthDay_movesToNextMonth() {
        val now = Instant.parse("2026-12-31T23:00:00+09:00")

        assertThat(nextReminderAt(ReminderTime(8, 0), now, seoul))
            .isEqualTo(Instant.parse("2027-01-01T08:00:00+09:00"))
    }

    // 2026-03-08 02:30 does not exist in New York: clocks jump from 02:00 to 03:00.
    @Test
    fun nextReminderAt_timeSkippedBySpringForward_movesByTheGap() {
        val now = Instant.parse("2026-03-07T23:00:00-05:00")

        assertThat(nextReminderAt(ReminderTime(2, 30), now, newYork))
            .isEqualTo(Instant.parse("2026-03-08T03:30:00-04:00"))
    }

    // 2026-11-01 01:30 happens twice in New York; the reminder comes at the first one.
    @Test
    fun nextReminderAt_timeRepeatedByFallBack_usesTheFirst() {
        val now = Instant.parse("2026-10-31T23:00:00-04:00")

        assertThat(nextReminderAt(ReminderTime(1, 30), now, newYork))
            .isEqualTo(Instant.parse("2026-11-01T01:30:00-04:00"))
    }

    @Test
    fun nextReminderAt_otherZone_followsThatZonesClock() {
        val now = Instant.parse("2026-11-07T12:00:00Z")

        assertThat(nextReminderAt(ReminderTime(20, 0), now, newYork))
            .isEqualTo(Instant.parse("2026-11-07T20:00:00-05:00"))
    }

    @Test
    fun reminderTimesToSave_unsortedWithDuplicate_keepsEachOnceInDayOrder() {
        val times = listOf(ReminderTime(20, 0), ReminderTime(8, 30), ReminderTime(20, 0))

        assertThat(reminderTimesToSave(times)).containsExactly(ReminderTime(8, 30), ReminderTime(20, 0))
    }

    @Test
    fun reminderTimesToSave_empty_isEmpty() {
        assertThat(reminderTimesToSave(emptyList())).isEmpty()
    }

    @Test
    fun reminderTimesToSave_threeDifferentTimes_throws() {
        val times = listOf(ReminderTime(7, 0), ReminderTime(12, 0), ReminderTime(20, 0))

        assertFailure { reminderTimesToSave(times) }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun reminderTime_outOfRange_throws() {
        assertFailure { ReminderTime(24, 0) }.isInstanceOf<IllegalArgumentException>()
        assertFailure { ReminderTime(0, 60) }.isInstanceOf<IllegalArgumentException>()
        assertFailure { ReminderTime(-1, 0) }.isInstanceOf<IllegalArgumentException>()
    }
}
