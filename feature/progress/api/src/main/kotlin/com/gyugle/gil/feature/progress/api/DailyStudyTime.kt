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
import kotlin.time.Duration.Companion.minutes

data class DailyStudyTime(val day: StudyDay, val duration: Duration) {
    /** Whether this day counts as a study day, toward the streak and the week. */
    val isStudyDay: Boolean get() = duration >= MIN_STUDY_TIME_PER_DAY
}

/** A day counts as a study day, toward the streak, with at least this much study. */
val MIN_STUDY_TIME_PER_DAY: Duration = 5.minutes
