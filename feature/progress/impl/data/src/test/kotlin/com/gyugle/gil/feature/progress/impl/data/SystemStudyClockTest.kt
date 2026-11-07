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
package com.gyugle.gil.feature.progress.impl.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.progress.api.StudyDay
import java.util.TimeZone as JavaTimeZone
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class SystemStudyClockTest {
    private val original = JavaTimeZone.getDefault()

    @AfterEach
    fun restoreZone() = JavaTimeZone.setDefault(original)

    @Test
    fun today_systemZoneChanges_nextCallUsesTheNewZone() {
        // 10:00 in Seoul, 17:00 the evening before in Los Angeles.
        val clock = SystemStudyClock(FixedClock(Instant.parse("2026-11-07T01:00:00Z")), TimeZone::currentSystemDefault)

        JavaTimeZone.setDefault(JavaTimeZone.getTimeZone("Asia/Seoul"))
        assertThat(clock.today()).isEqualTo(day("2026-11-07"))

        // What Android does for the app process when the user changes the time zone.
        JavaTimeZone.setDefault(JavaTimeZone.getTimeZone("America/Los_Angeles"))
        assertThat(clock.today()).isEqualTo(day("2026-11-06"))
    }

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }

    private fun day(iso: String) = StudyDay(LocalDate.parse(iso).toEpochDays())
}
