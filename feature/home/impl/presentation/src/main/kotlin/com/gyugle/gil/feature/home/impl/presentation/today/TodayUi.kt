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

/**
 * Today's plan as the screen shows it. [next] is the block the one main button opens; with
 * [firstLesson] it is named as the first lesson.
 */
@Immutable
data class TodayUi(
    val message: TodayMessage?,
    val studiedMinutes: Int,
    val goalMinutes: Int,
    val blocks: List<BlockUi>,
    val next: BlockUi?,
    val firstLesson: Boolean,
    val streak: Int,
    val week: List<DayUi>
)
