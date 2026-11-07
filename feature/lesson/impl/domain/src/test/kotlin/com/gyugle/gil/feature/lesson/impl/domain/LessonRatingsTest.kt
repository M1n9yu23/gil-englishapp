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
package com.gyugle.gil.feature.lesson.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import com.gyugle.gil.feature.progress.api.ReviewRating
import org.junit.jupiter.api.Test

class LessonRatingsTest {
    private val f = SoundId("sound.f")
    private val v = SoundId("sound.v")
    private val hello = SenseId("sense.hello.excl.greeting")

    private fun dictation(item: StudyItemId?) = LessonStep.Dictation(Sentence(1, "Fan.", null), item)

    /** Plays [steps] in order, answering each graded step with the next grades of [answers]. */
    private fun play(steps: List<LessonStep>, vararg answers: AnswerGrade): LessonRun {
        val grades = answers.iterator()
        var run = LessonRun.start(steps.size)
        while (!run.isFinished) {
            if (steps[run.currentStep!!] is LessonStep.Graded) run = run.answer(grades.next())
            run = run.next()
        }
        return run
    }

    @Test
    fun itemRatings_rightFirstTime_good() {
        val steps = listOf(dictation(f))

        assertThat(itemRatings(steps, play(steps, AnswerGrade.CORRECT))).isEqualTo(mapOf(f to ReviewRating.GOOD))
    }

    @Test
    fun itemRatings_oneLetterOff_hard() {
        val steps = listOf(dictation(f))

        assertThat(itemRatings(steps, play(steps, AnswerGrade.CLOSE))).isEqualTo(mapOf(f to ReviewRating.HARD))
    }

    @Test
    fun itemRatings_missedThenRightOnTheRetry_hard() {
        val steps = listOf(dictation(f))

        assertThat(itemRatings(steps, play(steps, AnswerGrade.WRONG, AnswerGrade.CORRECT)))
            .isEqualTo(mapOf(f to ReviewRating.HARD))
        assertThat(itemRatings(steps, play(steps, AnswerGrade.WRONG, AnswerGrade.CLOSE)))
            .isEqualTo(mapOf(f to ReviewRating.HARD))
    }

    @Test
    fun itemRatings_missedTwice_again() {
        val steps = listOf(dictation(f))

        assertThat(itemRatings(steps, play(steps, AnswerGrade.WRONG, AnswerGrade.WRONG)))
            .isEqualTo(mapOf(f to ReviewRating.AGAIN))
    }

    @Test
    fun itemRatings_missedAndLeftBeforeTheRetry_again() {
        val steps = listOf(dictation(f), dictation(v))
        val run = LessonRun.start(steps.size).answer(AnswerGrade.WRONG)

        assertThat(itemRatings(steps, run)).isEqualTo(mapOf(f to ReviewRating.AGAIN))
    }

    @Test
    fun itemRatings_sameItemTwice_keepsTheWorst() {
        val steps = listOf(dictation(f), dictation(f), dictation(f))

        assertThat(itemRatings(steps, play(steps, AnswerGrade.CORRECT, AnswerGrade.CLOSE, AnswerGrade.CORRECT)))
            .isEqualTo(mapOf(f to ReviewRating.HARD))
    }

    @Test
    fun itemRatings_stepsWithoutAnItem_leftOut() {
        val steps = listOf(LessonStep.Listen(Sentence(1, "Hi.", null)), dictation(null))

        assertThat(itemRatings(steps, play(steps, AnswerGrade.WRONG, AnswerGrade.WRONG))).isEqualTo(emptyMap())
    }

    @Test
    fun introducedRatings_onlyIntroducedItems_unaskedOnesGood() {
        // v comes from an earlier lesson, so it is rated but not part of this lesson's cards.
        val steps = listOf(dictation(f), dictation(v))

        assertThat(
            introducedRatings(
                listOf(f, hello),
                steps,
                play(steps, AnswerGrade.WRONG, AnswerGrade.CORRECT, AnswerGrade.WRONG)
            )
        ).isEqualTo(mapOf(f to ReviewRating.AGAIN, hello to ReviewRating.GOOD))
    }

    @Test
    fun stepsThatNeedSound_minimalPairAndDictationOnly() {
        val steps = listOf(
            LessonStep.Listen(Sentence(1, "Hi.", null)),
            LessonStep.MinimalPair(emptyList(), answer = 0, why = null, item = null),
            dictation(null),
            LessonStep.ListenChoose(Sentence(2, "fan", null), emptyList(), item = null)
        )

        assertThat(stepsThatNeedSound(steps)).isEqualTo(setOf(1, 2))
    }
}
