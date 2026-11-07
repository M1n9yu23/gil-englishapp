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
package com.gyugle.gil.feature.home.impl.domain.model

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonOverview

/**
 * What to study today, in order: [review] up to today's share of the goal, then [lesson], then
 * [moreReview] when more cards are due than that share. [next] is what "continue" opens.
 *
 * @property reviewDoneToday whether reviews were done today and none are left.
 * @property reviewHeavy whether today's reviews take more than their share of the goal, so the
 *   plan puts reviews first and holds back new words.
 * @property goalMet whether today's study time has reached the goal.
 * @property firstDay whether the learner has no record yet.
 */
data class TodayPlan(
    val review: ReviewBlock?,
    val reviewDoneToday: Boolean,
    val lesson: LessonBlock?,
    val moreReview: ReviewBlock?,
    val next: NextStep?,
    val reviewHeavy: Boolean,
    val goalMet: Boolean,
    val firstDay: Boolean
) {
    /** Every lesson of the course is done. */
    val courseDone: Boolean get() = lesson == null
}

/** [count] cards due, about [minutes] long. */
data class ReviewBlock(val count: Int, val minutes: Int)

/**
 * The next lesson in course order. [recommended] is false when it would bring more new items than
 * today has room for; it still opens, the plan just does not lead with it.
 */
data class LessonBlock(val lesson: LessonOverview, val recommended: Boolean)

sealed interface NextStep {
    data class Review(val limit: Int) : NextStep

    data class Lesson(val id: LessonId) : NextStep
}
