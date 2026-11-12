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
package com.gyugle.gil.feature.curriculum.impl.presentation.path

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.impl.domain.model.Stage
import com.gyugle.gil.feature.curriculum.impl.presentation.toLocalizedTextUi
import com.gyugle.gil.feature.review.api.hasStageTest
import com.gyugle.gil.feature.review.api.stageTestQuestions

/**
 * Maps the stages and marks where the learner is: at the first stage with a lesson not in
 * [completed]. With every lesson finished, that is the stage after the last one with lessons, the
 * next to come. A stage with lessons left can be tested out of when it has enough items.
 */
fun List<Stage>.toStageUis(completed: Set<LessonId>): List<StageUi> {
    val unfinished = indexOfFirst { stage -> stage.lessons.any { it !in completed } }
    val current = if (unfinished >= 0) {
        unfinished
    } else {
        (indexOfLast { it.lessons.isNotEmpty() } + 1).coerceAtMost(lastIndex)
    }
    return mapIndexed { index, stage ->
        StageUi(
            id = stage.id.value,
            title = stage.title.toLocalizedTextUi(),
            canDo = stage.canDo.map { it.toLocalizedTextUi() },
            units = stage.units.map {
                UnitRowUi(id = it.id, title = it.title.toLocalizedTextUi(), lessonCount = it.lessonCount)
            },
            place = when {
                index < current -> StagePlace.PASSED
                index == current -> StagePlace.CURRENT
                else -> StagePlace.AHEAD
            },
            testQuestions = stageTestQuestions(stage.itemCount).takeIf {
                hasStageTest(stage.itemCount) && stage.lessons.any { it !in completed }
            }
        )
    }
}
