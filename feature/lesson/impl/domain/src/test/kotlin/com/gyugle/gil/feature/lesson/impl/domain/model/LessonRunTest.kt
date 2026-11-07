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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class LessonRunTest {

    @Test
    fun start_playsEveryStepInOrder() {
        val run = LessonRun.start(stepCount = 3)

        assertThat(run.queue).isEqualTo(listOf(0, 1, 2))
        assertThat(run.currentStep).isEqualTo(0)
        assertThat(run.isRetry).isFalse()
        assertThat(run.isAnswered).isFalse()
    }

    @Test
    fun start_skippedSteps_leftOutOfTheQueue() {
        val run = LessonRun.start(stepCount = 4, skipped = setOf(1, 3))

        assertThat(run.queue).isEqualTo(listOf(0, 2))
        assertThat(run.firstPassSize).isEqualTo(2)
    }

    @Test
    fun answer_wrongFirstTry_queuesTheStepAgainAtTheEnd() {
        val run = LessonRun.start(stepCount = 2).answer(AnswerGrade.WRONG)

        assertThat(run.queue).isEqualTo(listOf(0, 1, 0))
        assertThat(run.outcomes).isEqualTo(mapOf(0 to StepOutcome(AnswerGrade.WRONG)))
        assertThat(run.isAnswered).isTrue()
    }

    @Test
    fun answer_rightOrCloseFirstTry_doesNotQueueAgain() {
        assertThat(LessonRun.start(stepCount = 2).answer(AnswerGrade.CORRECT).queue).isEqualTo(listOf(0, 1))
        assertThat(LessonRun.start(stepCount = 2).answer(AnswerGrade.CLOSE).queue).isEqualTo(listOf(0, 1))
    }

    @Test
    fun answer_twiceOnOnePass_keepsTheFirstAnswer() {
        val run = LessonRun.start(stepCount = 1).answer(AnswerGrade.WRONG).answer(AnswerGrade.CORRECT)

        assertThat(run.outcomes.getValue(0)).isEqualTo(StepOutcome(AnswerGrade.WRONG))
        assertThat(run.queue).isEqualTo(listOf(0, 0))
    }

    @Test
    fun answer_onTheRetry_recordsItAndNeverQueuesAThirdTime() {
        val run = LessonRun.start(stepCount = 2)
            .answer(AnswerGrade.WRONG).next()
            .next()

        assertThat(run.currentStep).isEqualTo(0)
        assertThat(run.isRetry).isTrue()
        assertThat(run.isAnswered).isFalse()

        val answered = run.answer(AnswerGrade.WRONG)
        assertThat(answered.outcomes.getValue(0)).isEqualTo(StepOutcome(AnswerGrade.WRONG, retry = AnswerGrade.WRONG))
        assertThat(answered.queue).isEqualTo(listOf(0, 1, 0))
        assertThat(answered.isAnswered).isTrue()
        assertThat(answered.next().isFinished).isTrue()
    }

    @Test
    fun retry_afterSkippedSteps_isTheRetryNotAFirstPass() {
        val run = LessonRun.start(stepCount = 3, skipped = setOf(2))
            .answer(AnswerGrade.WRONG).next()
            .next()

        assertThat(run.currentStep).isEqualTo(0)
        assertThat(run.isRetry).isTrue()
    }

    @Test
    fun next_pastTheLastStep_finishedAndStaysFinished() {
        val run = LessonRun.start(stepCount = 1).next()

        assertThat(run.isFinished).isTrue()
        assertThat(run.currentStep).isNull()
        assertThat(run.next()).isEqualTo(run)
        assertThat(run.answer(AnswerGrade.CORRECT)).isEqualTo(run)
    }

    @Test
    fun answer_retryWithoutAFirstAnswer_recordsItAsFirst() {
        // Only a damaged saved run looks like this; it must not crash.
        val run = LessonRun(
            stepCount = 1,
            firstPassSize = 1,
            queue = listOf(0, 0),
            position = 1,
            outcomes = emptyMap(),
            studiedMillis = 0
        )

        assertThat(run.answer(AnswerGrade.CORRECT).outcomes).isEqualTo(mapOf(0 to StepOutcome(AnswerGrade.CORRECT)))
    }

    @Test
    fun studied_addsPositiveTimeOnly() {
        val run = LessonRun.start(stepCount = 1).studied(1_500).studied(0).studied(-20).studied(500)

        assertThat(run.studiedMillis).isEqualTo(2_000)
    }
}
