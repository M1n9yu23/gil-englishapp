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

import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.impl.domain.studyDayOf
import com.gyugle.gil.feature.progress.impl.domain.studyDayStart
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone

/**
 * The device clock and time zone.
 *
 * The zone is read on every call. When the user changes the time zone, Android resets the app
 * process's default zone (ACTION_TIMEZONE_CHANGED), so the next call already uses the new one.
 */
class SystemStudyClock internal constructor(private val clock: Clock, private val zone: () -> TimeZone) : StudyClock {
    @Inject
    constructor() : this(Clock.System, TimeZone::currentSystemDefault)

    override fun now(): Instant = clock.now()

    override fun dayOf(instant: Instant): StudyDay = studyDayOf(instant, zone())

    override fun startOf(day: StudyDay): Instant = studyDayStart(day, zone())
}
