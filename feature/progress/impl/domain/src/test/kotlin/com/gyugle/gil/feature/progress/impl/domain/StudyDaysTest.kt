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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.progress.api.StudyDay
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class StudyDaysTest {
    @Nested
    inner class StudyDayOf {
        private val seoul = TimeZone.of("Asia/Seoul")

        @Test
        fun beforeFourInTheMorning_isTheDayBefore() {
            assertThat(studyDayOf(Instant.parse("2026-11-07T03:59:59+09:00"), seoul)).isEqualTo(day("2026-11-06"))
        }

        @Test
        fun atFourInTheMorning_isTheNewDay() {
            assertThat(studyDayOf(Instant.parse("2026-11-07T04:00:00+09:00"), seoul)).isEqualTo(day("2026-11-07"))
        }

        @Test
        fun lateEvening_isThatDay() {
            assertThat(studyDayOf(Instant.parse("2026-11-07T23:59:59+09:00"), seoul)).isEqualTo(day("2026-11-07"))
        }

        @Test
        fun sameInstantInAnotherZone_followsThatZonesClock() {
            // 10:00 in Seoul is 17:00 the evening before in Los Angeles.
            val instant = Instant.parse("2026-11-07T10:00:00+09:00")

            assertThat(studyDayOf(instant, TimeZone.of("America/Los_Angeles"))).isEqualTo(day("2026-11-06"))
        }

        @Test
        fun nightClocksGoBack_hourBeforeDayStartStillBelongsToTheDayBefore() {
            // Daylight saving ends in New York at 02:00 on 2026-11-01, so 01:30 happens twice.
            val newYork = TimeZone.of("America/New_York")

            assertThat(studyDayOf(Instant.parse("2026-11-01T01:30:00-04:00"), newYork)).isEqualTo(day("2026-10-31"))
            assertThat(studyDayOf(Instant.parse("2026-11-01T01:30:00-05:00"), newYork)).isEqualTo(day("2026-10-31"))
            assertThat(studyDayOf(Instant.parse("2026-11-01T04:00:00-05:00"), newYork)).isEqualTo(day("2026-11-01"))
        }
    }

    @Nested
    inner class StudyDayStart {
        private val seoul = TimeZone.of("Asia/Seoul")

        @Test
        fun isFourInTheMorningOfThatDate() {
            assertThat(studyDayStart(day("2026-11-07"), seoul)).isEqualTo(Instant.parse("2026-11-07T04:00:00+09:00"))
        }

        @Test
        fun belongsToThatDay_andTheSecondBeforeToTheDayBefore() {
            val start = studyDayStart(day("2026-11-07"), seoul)

            assertThat(studyDayOf(start, seoul)).isEqualTo(day("2026-11-07"))
            assertThat(studyDayOf(start - 1.seconds, seoul)).isEqualTo(day("2026-11-06"))
        }

        @Test
        fun dayClocksGoForward_followsTheNewOffset() {
            // Daylight saving starts in New York at 02:00 on 2026-03-08, two hours before the day does.
            val newYork = TimeZone.of("America/New_York")

            assertThat(studyDayStart(day("2026-03-08"), newYork)).isEqualTo(Instant.parse("2026-03-08T04:00:00-04:00"))
        }
    }

    @Nested
    inner class StudyStreak {
        private val today = StudyDay(1_000)

        @Test
        fun noStudy_isZero() {
            assertThat(studyStreak(emptySet(), today)).isEqualTo(0)
        }

        @Test
        fun everyDayIncludingToday_countsEachDay() {
            assertThat(studyStreak(daysBack(0..4), today)).isEqualTo(5)
        }

        @Test
        fun todayNotStudiedYet_keepsYesterdaysStreak() {
            assertThat(studyStreak(daysBack(1..4), today)).isEqualTo(4)
        }

        @Test
        fun twoDaysOffInARow_keepsTheStreak() {
            assertThat(studyStreak(daysBack(0..0) + daysBack(3..6), today)).isEqualTo(5)
        }

        @Test
        fun thirdDayOffInARow_endsTheStreak() {
            assertThat(studyStreak(daysBack(0..0) + daysBack(4..10), today)).isEqualTo(1)
        }

        @Test
        fun thirdDayOffWithinSevenDays_endsTheStreak() {
            // Days off: 1, 3 and 5 days ago, all within seven days.
            val studied = daysBack(0..0) + daysBack(2..2) + daysBack(4..4) + daysBack(6..9)

            assertThat(studyStreak(studied, today)).isEqualTo(3)
        }

        @Test
        fun daysOffOneTwoAndSevenDaysAgo_shareASevenDayWindow_endTheStreak() {
            val studied = daysBack(0..0) + daysBack(3..6) + daysBack(8..10)

            assertThat(studyStreak(studied, today)).isEqualTo(5)
        }

        @Test
        fun daysOffOneTwoAndEightDaysAgo_neverShareAWindow_keepTheStreak() {
            val studied = daysBack(0..0) + daysBack(3..7) + daysBack(9..10)

            assertThat(studyStreak(studied, today)).isEqualTo(8)
        }

        @Test
        fun daysOffSpreadOverMoreThanSevenDays_keepTheStreak() {
            // Days off every fourth day: never three within seven days.
            val studied = (0..20).filter { it % 4 != 3 }.map { today - it }.toSet()

            assertThat(studyStreak(studied, today)).isEqualTo(studied.size)
        }

        @Test
        fun threeDaysOffBeforeToday_andTodayNotStudied_isZero() {
            assertThat(studyStreak(daysBack(4..8), today)).isEqualTo(0)
        }

        @Test
        fun studyRecordedAfterToday_isIgnored() {
            assertThat(studyStreak(daysBack(0..1) + setOf(today + 1), today)).isEqualTo(2)
        }

        private fun daysBack(range: IntRange): Set<StudyDay> = range.map { today - it }.toSet()
    }

    private fun day(iso: String) = StudyDay(LocalDate.parse(iso).toEpochDays())
}
