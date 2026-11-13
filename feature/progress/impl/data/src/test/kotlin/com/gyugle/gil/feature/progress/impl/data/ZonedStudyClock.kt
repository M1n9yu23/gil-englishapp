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

import com.gyugle.gil.feature.progress.impl.domain.studyDayOf
import com.gyugle.gil.feature.progress.impl.domain.studyDayStart
import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/** A clock the test moves by hand, with the real 04:00 day boundary. */
class FakeStudyClock(var now: Instant, var zone: TimeZone = TimeZone.UTC) : StudyClock {
    override fun now(): Instant = now

    override fun dayOf(instant: Instant): StudyDay = studyDayOf(instant, zone)

    override fun startOf(day: StudyDay): Instant = studyDayStart(day, zone)

    fun advanceBy(duration: Duration) {
        now += duration
    }
}
