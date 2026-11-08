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

/**
 * The learner's record as the Stats tab shows it. Minutes are whole minutes, rounded down like the
 * Today tab. [forecast] starts with today.
 */
@Immutable
data class StatsUi(
    val streakDays: Int,
    val weekMinutes: Int,
    val monthMinutes: Int,
    val calendar: CalendarUi,
    val forecast: List<ForecastDayUi>,
    val levels: LevelsUi,
    val stages: List<StageUi>
)
