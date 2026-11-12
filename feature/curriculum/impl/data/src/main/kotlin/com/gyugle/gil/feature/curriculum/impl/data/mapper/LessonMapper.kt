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
package com.gyugle.gil.feature.curriculum.impl.data.mapper

import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.impl.data.dao.LessonItemRow
import com.gyugle.gil.feature.curriculum.impl.data.dto.StepPayloadDto
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonEntity
import com.gyugle.gil.feature.curriculum.impl.domain.model.LessonSummary

fun LessonEntity.toLessonSummary(): LessonSummary = LessonSummary(
    id = LessonId(id),
    title = LocalizedText(ko = titleKo, en = titleEn),
    goal = LocalizedText(ko = goalKo, en = goalEn)
)

fun LessonEntity.toLesson(steps: List<StepPayloadDto>, items: List<LessonItemRow>, next: String?): Lesson = Lesson(
    id = LessonId(id),
    title = LocalizedText(ko = titleKo, en = titleEn),
    goal = LocalizedText(ko = goalKo, en = goalEn),
    steps = steps.map { it.toLessonStep() },
    introduced = items.map { it.toLessonItem() },
    next = next?.let(::LessonId)
)

fun LessonItemRow.toLessonItem(): LessonItem =
    LessonItem(id = id.toStudyItemId(), text = text, ipa = ipa, meaningKo = meaningKo)
