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
package com.gyugle.gil.feature.stats.impl.presentation.stats

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek

/**
 * One month of study days. [weeks] are rows of seven in [weekdays] order, with null cells outside
 * the month. [hasPrevious] and [hasNext] say whether the learner can move to the month before or
 * after: back to the first month with study time, forward to this month.
 */
@Immutable
data class CalendarUi(
    val year: Int,
    val month: Int,
    val weekdays: List<DayOfWeek>,
    val weeks: List<List<CalendarDayUi?>>,
    val studiedDays: Int,
    val hasPrevious: Boolean,
    val hasNext: Boolean
)
