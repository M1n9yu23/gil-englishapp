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

import com.gyugle.gil.feature.curriculum.impl.domain.model.Stage
import com.gyugle.gil.feature.curriculum.impl.presentation.toLocalizedTextUi

/**
 * Maps the stages and marks where the learner is. Until learning progress exists (M3), every
 * learner is at the start: the first stage that has units, or the first stage when none has.
 */
fun List<Stage>.toStageUis(): List<StageUi> {
    val current = indexOfFirst { it.units.isNotEmpty() }.coerceAtLeast(0)
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
            }
        )
    }
}
