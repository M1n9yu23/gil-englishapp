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
package com.gyugle.gil.feature.stats.impl.domain

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.StageLessons
import com.gyugle.gil.feature.stats.impl.domain.model.StageProgress

/** Progress through each stage that has lessons, in course order. Stages still to come are left out. */
fun stageProgress(stages: List<StageLessons>, completed: Set<LessonId>): List<StageProgress> = stages
    .filter { it.lessons.isNotEmpty() }
    .map { stage -> StageProgress(stage.stage, stage.lessons.count { it in completed }, stage.lessons.size) }
