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
package com.gyugle.gil.feature.lesson.impl.data.dto

import kotlinx.serialization.Serializable

/**
 * A lesson run as stored in lesson_prefs. Grades are stored by name; [outcomes] is keyed by step
 * index. Renaming a field here loses the saved places, which only means lessons start over.
 */
@Serializable
data class LessonRunDto(
    val stepCount: Int,
    val firstPassSize: Int,
    val queue: List<Int>,
    val position: Int,
    val outcomes: Map<Int, StepOutcomeDto>,
    val studiedMillis: Long
)
