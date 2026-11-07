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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import androidx.compose.runtime.Immutable

/**
 * The end of a lesson. [items] are what it introduced; [isSaving] and [saveFailed] follow writing
 * the result to the learner's record.
 */
@Immutable
data class ResultUi(
    val minutes: Int,
    val goal: LocalizedTextUi,
    val items: List<ResultItemUi>,
    val hasNext: Boolean,
    val isSaving: Boolean,
    val saveFailed: Boolean
)
