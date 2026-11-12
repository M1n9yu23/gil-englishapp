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
package com.gyugle.gil.feature.home.impl.presentation

import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.home.api.ContinueFinder
import com.gyugle.gil.feature.home.impl.domain.introducedOn
import com.gyugle.gil.feature.home.impl.domain.model.NextStep
import com.gyugle.gil.feature.home.impl.domain.planToday
import com.gyugle.gil.feature.home.impl.domain.reviewedOn
import com.gyugle.gil.feature.lesson.api.LessonRoute
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.review.api.ReviewSessionRoute
import javax.inject.Inject
import kotlin.time.Duration
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import timber.log.Timber

/** Reads the record once and plans today as the Today tab does, so both open the same step. */
internal class TodayContinueFinder @Inject constructor(
    private val goals: GoalObserver,
    private val progress: ProgressObserver,
    private val course: CourseObserver,
    private val clock: StudyClock
) : ContinueFinder {

    override suspend fun find(): NavKey? = try {
        when (val step = nextStep()) {
            is NextStep.Review -> ReviewSessionRoute(step.limit)
            is NextStep.Lesson -> LessonRoute(step.id)
            null -> null
        }
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Timber.e(e, "Could not read the study record")
        null
    }

    private suspend fun nextStep(): NextStep? {
        val today = clock.today()
        val cards = progress.observeCards().first()
        val completed = progress.observeCompletedLessons().first()
        val nextLesson = when (val read = course.getNextLesson(completed)) {
            is Outcome.Success -> read.data
            is Outcome.Failure -> return null
        }
        val plan = planToday(
            goal = goals.observeGoal().first(),
            studiedToday = progress.observeStudyTime(today, today).first().firstOrNull()?.duration ?: Duration.ZERO,
            dueCount = progress.observeDueCards(today).first().size,
            reviewedToday = reviewedOn(today, cards),
            introducedToday = introducedOn(today, cards),
            nextLesson = nextLesson,
            hasRecord = cards.isNotEmpty() || completed.isNotEmpty()
        )
        return plan.next
    }
}
