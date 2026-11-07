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
package com.gyugle.gil.feature.home.impl.domain

import com.gyugle.gil.feature.curriculum.api.LessonOverview
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.home.impl.domain.model.LessonBlock
import com.gyugle.gil.feature.home.impl.domain.model.NextStep
import com.gyugle.gil.feature.home.impl.domain.model.ReviewBlock
import com.gyugle.gil.feature.home.impl.domain.model.TodayPlan
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.review.api.REVIEW_TIME_PER_CARD
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Reviews take at most this share of the daily goal before the plan turns to new lessons. */
internal const val REVIEW_SHARE_OF_GOAL = 0.5

/** New items a day may bring, per minute of the goal: 30 minutes allow 18. */
internal const val NEW_ITEMS_PER_GOAL_MINUTE = 0.6

/**
 * Today's plan. Reviews come first, up to [REVIEW_SHARE_OF_GOAL] of the goal counting those done
 * already today; then the next lesson; then the rest of the reviews. Cards are never brought
 * forward: only cards due today are reviewed, even once the goal is met.
 *
 * New items are held back as reviews pile up: the day's room for them, [NEW_ITEMS_PER_GOAL_MINUTE]
 * per goal minute, shrinks by one for every card beyond the review share. The next lesson is
 * recommended while it fits that room, and the first lesson of a day always fits if any room is
 * left, since a lesson cannot be split.
 *
 * @param dueCount cards due today or earlier.
 * @param reviewedToday cards reviewed today, not counting items first met in a lesson today.
 * @param introducedToday items first met in a lesson today.
 * @param nextLesson the first lesson not completed, null when the course is done.
 * @param hasRecord whether the learner has any card or completed lesson.
 */
fun planToday(
    goal: DailyGoal,
    studiedToday: Duration,
    dueCount: Int,
    reviewedToday: Int,
    introducedToday: Int,
    nextLesson: LessonOverview?,
    hasRecord: Boolean
): TodayPlan {
    val reviewShare = (goal.duration * REVIEW_SHARE_OF_GOAL / REVIEW_TIME_PER_CARD).toInt()
    val shareLeft = (reviewShare - reviewedToday).coerceAtLeast(0)
    val firstCount = minOf(dueCount, shareLeft)
    val review = firstCount.takeIf { it > 0 }?.let(::reviewBlock)
    val moreReview = (dueCount - firstCount).takeIf { it > 0 }?.let(::reviewBlock)

    val backlog = (reviewedToday + dueCount - reviewShare).coerceAtLeast(0)
    val newItemRoom = ((goal.minutes * NEW_ITEMS_PER_GOAL_MINUTE).toInt() - backlog).coerceAtLeast(0)
    val lesson = nextLesson?.let {
        val fits = introducedToday == 0 || introducedToday + it.newItemCount <= newItemRoom
        LessonBlock(it, recommended = newItemRoom > 0 && fits)
    }

    val next = when {
        review != null -> NextStep.Review(review.count)
        lesson?.recommended == true -> NextStep.Lesson(lesson.lesson.id)
        moreReview != null -> NextStep.Review(moreReview.count)
        lesson != null -> NextStep.Lesson(lesson.lesson.id)
        else -> null
    }
    return TodayPlan(
        review = review,
        reviewDoneToday = dueCount == 0 && reviewedToday > 0,
        lesson = lesson,
        moreReview = moreReview,
        next = next,
        reviewHeavy = reviewedToday + dueCount > reviewShare,
        goalMet = goal.isMetBy(studiedToday),
        firstDay = !hasRecord
    )
}

private fun reviewBlock(count: Int) = ReviewBlock(count, minutesFor(count))

/** Rounded up and at least one, as a reminder says it, so a few cards never take "about 0 minutes". */
private fun minutesFor(cards: Int): Int {
    val total = REVIEW_TIME_PER_CARD * cards
    val whole = total.inWholeMinutes.toInt()
    return if (total > whole.minutes) whole + 1 else whole.coerceAtLeast(1)
}

/**
 * Cards reviewed on [today], from their schedule: a card last rated today after the lesson that
 * introduced it. A card's first rating comes from that lesson, so a review count of one means no
 * review yet.
 */
fun reviewedOn(today: StudyDay, cards: List<CardState>): Int =
    cards.count { it.lastReviewDay == today && it.reviewCount > 1 }

/** Items first met on [today]: rated once, today, by the lesson that introduced them. */
fun introducedOn(today: StudyDay, cards: List<CardState>): Int =
    cards.count { it.lastReviewDay == today && it.reviewCount == 1 }
