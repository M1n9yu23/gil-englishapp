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

import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.model.LessonStep
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import com.gyugle.gil.feature.lesson.impl.domain.model.StepOutcome
import com.gyugle.gil.feature.progress.api.ReviewRating

/**
 * How the learner did on each item the graded steps of [run] asked about, from earlier lessons
 * too. An item asked about more than once gets its worst rating.
 *
 * Right on the first try is good. One letter off, or right when the step came back after a miss,
 * is hard. Missed twice is again. Answer speed does not count: a timer would only add pressure.
 */
fun itemRatings(steps: List<LessonStep>, run: LessonRun): Map<StudyItemId, ReviewRating> = run.outcomes
    .mapNotNull { (index, outcome) ->
        val item = (steps.getOrNull(index) as? LessonStep.Graded)?.item ?: return@mapNotNull null
        item to outcome.toRating()
    }
    .groupBy({ it.first }, { it.second })
    .mapValues { (_, ratings) ->
        when {
            ReviewRating.AGAIN in ratings -> ReviewRating.AGAIN
            ReviewRating.HARD in ratings -> ReviewRating.HARD
            else -> ReviewRating.GOOD
        }
    }

/**
 * The first rating of each item the lesson [introduced], for `completeLesson`. An item no graded
 * step asked about was only shown, which counts as good.
 */
fun introducedRatings(
    introduced: List<StudyItemId>,
    steps: List<LessonStep>,
    run: LessonRun
): Map<StudyItemId, ReviewRating> {
    val rated = itemRatings(steps, run)
    return introduced.associateWith { rated[it] ?: ReviewRating.GOOD }
}

private fun StepOutcome.toRating(): ReviewRating = when (first) {
    AnswerGrade.CORRECT -> ReviewRating.GOOD
    AnswerGrade.CLOSE -> ReviewRating.HARD
    AnswerGrade.WRONG -> if (retry?.isAccepted == true) ReviewRating.HARD else ReviewRating.AGAIN
}
