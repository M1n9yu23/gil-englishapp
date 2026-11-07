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
package com.gyugle.gil.feature.curriculum.api.model

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LocalizedText

/**
 * A lesson with everything needed to play it. [introduced] are the items it teaches for the first
 * time, in the order the content lists them. [next] is the lesson after it in the course, or null
 * for the last one.
 */
data class Lesson(
    val id: LessonId,
    val title: LocalizedText,
    val goal: LocalizedText,
    val steps: List<LessonStep>,
    val introduced: List<LessonItem>,
    val next: LessonId?
)
