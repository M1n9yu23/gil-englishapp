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

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageCanDoEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageEntity
import com.gyugle.gil.feature.curriculum.impl.domain.model.Stage
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitSummary

fun StageEntity.toStage(
    canDo: List<StageCanDoEntity>,
    units: List<UnitSummary>,
    lessons: List<LessonId>,
    itemCount: Int
): Stage = Stage(
    id = StageId(id),
    title = localizedTitle(),
    canDo = canDo.map { LocalizedText(ko = it.textKo, en = it.textEn) },
    units = units,
    lessons = lessons,
    itemCount = itemCount
)

fun StageEntity.localizedTitle(): LocalizedText = LocalizedText(ko = titleKo, en = titleEn)
