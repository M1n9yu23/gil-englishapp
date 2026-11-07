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
package com.gyugle.gil.feature.home.impl.presentation.today

import androidx.compose.runtime.Immutable
import java.time.DayOfWeek

/**
 * A day of this week: whether it is a study day and whether it is today. A [muted] day is still
 * to come, or came before the learner started, so it cannot have been missed.
 */
@Immutable
data class DayUi(val dayOfWeek: DayOfWeek, val studied: Boolean, val isToday: Boolean, val muted: Boolean)
