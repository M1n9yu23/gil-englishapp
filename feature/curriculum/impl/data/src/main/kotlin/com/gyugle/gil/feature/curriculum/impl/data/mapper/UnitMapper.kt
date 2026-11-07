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

import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.UnitEntity
import com.gyugle.gil.feature.curriculum.impl.domain.model.LocalizedText
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitDetail
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitSummary

fun UnitEntity.toUnitSummary(lessonCount: Int): UnitSummary =
    UnitSummary(id = UnitId(id), title = LocalizedText(ko = titleKo, en = titleEn), lessonCount = lessonCount)

fun UnitEntity.toUnitDetail(stage: StageEntity, lessons: List<LessonEntity>): UnitDetail = UnitDetail(
    id = UnitId(id),
    stageTitle = stage.localizedTitle(),
    title = LocalizedText(ko = titleKo, en = titleEn),
    lessons = lessons.map { it.toLessonSummary() }
)
