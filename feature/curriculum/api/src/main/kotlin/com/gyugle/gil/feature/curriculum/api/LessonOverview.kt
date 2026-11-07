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
package com.gyugle.gil.feature.curriculum.api

/**
 * A lesson as a study plan lists it, without its steps. [newItemCount] is how many items it
 * introduces; [stepCount] stands in for its length, at about a minute a step.
 */
data class LessonOverview(
    val id: LessonId,
    val title: LocalizedText,
    val unitTitle: LocalizedText,
    val stepCount: Int,
    val newItemCount: Int
)
