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
package com.gyugle.gil.feature.lesson.impl.data.mapper

import com.gyugle.gil.feature.lesson.impl.data.dto.LessonRunDto
import com.gyugle.gil.feature.lesson.impl.data.dto.StepOutcomeDto
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import com.gyugle.gil.feature.lesson.impl.domain.model.StepOutcome

fun LessonRun.toLessonRunDto(): LessonRunDto = LessonRunDto(
    stepCount = stepCount,
    firstPassSize = firstPassSize,
    queue = queue,
    position = position,
    outcomes = outcomes.mapValues { (_, outcome) -> StepOutcomeDto(outcome.first.name, outcome.retry?.name) },
    studiedMillis = studiedMillis
)

/**
 * The run, or null when the stored one cannot be trusted: an unknown grade, or an index outside
 * its own steps, as a hand-edited file or an older build could leave.
 */
fun LessonRunDto.toLessonRunOrNull(): LessonRun? {
    val consistent = stepCount >= 0 &&
        firstPassSize in 0..queue.size &&
        position in 0..queue.size &&
        queue.all { it in 0 until stepCount } &&
        outcomes.keys.all { it in 0 until stepCount } &&
        studiedMillis >= 0
    if (!consistent) return null
    val mapped = outcomes.mapValues { (_, dto) ->
        val first = dto.first.toGradeOrNull() ?: return null
        val retry = dto.retry?.let { it.toGradeOrNull() ?: return null }
        StepOutcome(first, retry)
    }
    return LessonRun(stepCount, firstPassSize, queue, position, mapped, studiedMillis)
}

private fun String.toGradeOrNull(): AnswerGrade? = AnswerGrade.entries.firstOrNull { it.name == this }
