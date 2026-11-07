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

import kotlin.time.Instant

/** The current moment and the [StudyDay] it falls on. Tests replace it to move time. */
interface StudyClock {
    fun now(): Instant

    /** The study day [instant] falls on in the device's current time zone. */
    fun dayOf(instant: Instant): StudyDay

    fun today(): StudyDay = dayOf(now())
}
