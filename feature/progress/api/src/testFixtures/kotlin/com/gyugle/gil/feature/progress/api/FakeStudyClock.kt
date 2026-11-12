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

import kotlin.time.Duration
import kotlin.time.Instant

/**
 * A clock that moves only when a test moves it. Its days start at midnight UTC: the 04:00 boundary
 * belongs to the real clock and is tested there.
 */
class FakeStudyClock(var now: Instant = Instant.parse("2026-11-07T10:00:00Z")) : StudyClock {
    override fun now(): Instant = now

    override fun dayOf(instant: Instant): StudyDay = StudyDay(instant.epochSeconds / SECONDS_PER_DAY)

    override fun startOf(day: StudyDay): Instant = Instant.fromEpochSeconds(day.epochDay * SECONDS_PER_DAY)

    fun advanceBy(duration: Duration) {
        now += duration
    }

    private companion object {
        const val SECONDS_PER_DAY = 86_400L
    }
}
