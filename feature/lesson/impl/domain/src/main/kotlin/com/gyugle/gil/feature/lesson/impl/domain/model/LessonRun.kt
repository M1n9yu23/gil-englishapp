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
package com.gyugle.gil.feature.lesson.impl.domain.model

import kotlinx.serialization.Serializable

/**
 * Where the learner is in one lesson and how the answers went, saved so that a lesson left halfway
 * opens at the same step.
 *
 * Steps play in [queue] order, by index into the lesson's steps. A graded step missed on its first
 * try is added to the end once, so it comes back before the lesson ends. [position] points into
 * [queue]; it equals the queue's size when every step is done.
 *
 * @property stepCount the lesson's step count when the run started. A saved run whose count no
 * longer matches belongs to content that has changed and is dropped.
 * @property firstPassSize how many steps the first pass plays; the queue past it holds the retries.
 * @property studiedMillis time spent on the lesson so far, for the result.
 */
@Serializable
data class LessonRun(
    val stepCount: Int,
    val firstPassSize: Int,
    val queue: List<Int>,
    val position: Int,
    val outcomes: Map<Int, StepOutcome>,
    val studiedMillis: Long
) {
    /** The index of the step being played, or null when the run is over. */
    val currentStep: Int? get() = queue.getOrNull(position)

    val isFinished: Boolean get() = position >= queue.size

    /** True while playing a step a second time, after a miss. */
    val isRetry: Boolean get() = position >= firstPassSize

    /** True once the step being played has its answer for this pass, before moving on. */
    val isAnswered: Boolean
        get() {
            val outcome = currentStep?.let(outcomes::get) ?: return false
            return !isRetry || outcome.retry != null
        }

    /**
     * Records the answer to the current step. A first miss queues the step again at the end.
     * Answering the same pass twice changes nothing.
     */
    fun answer(grade: AnswerGrade): LessonRun {
        val step = currentStep ?: return this
        if (isAnswered) return this
        return if (isRetry) {
            copy(outcomes = outcomes + (step to outcomes.getValue(step).copy(retry = grade)))
        } else {
            copy(
                queue = if (grade == AnswerGrade.WRONG) queue + step else queue,
                outcomes = outcomes + (step to StepOutcome(first = grade))
            )
        }
    }

    /** Moves to the next step. Past the last one the run is [isFinished]. */
    fun next(): LessonRun = if (isFinished) this else copy(position = position + 1)

    /** Adds time spent on the lesson. */
    fun studied(millis: Long): LessonRun = if (millis <= 0) this else copy(studiedMillis = studiedMillis + millis)

    companion object {
        /** A new run over every step except [skipped], such as listening steps with no voice. */
        fun start(stepCount: Int, skipped: Set<Int> = emptySet()): LessonRun {
            val queue = (0 until stepCount).filterNot { it in skipped }
            return LessonRun(
                stepCount = stepCount,
                firstPassSize = queue.size,
                queue = queue,
                position = 0,
                outcomes = emptyMap(),
                studiedMillis = 0
            )
        }
    }
}
