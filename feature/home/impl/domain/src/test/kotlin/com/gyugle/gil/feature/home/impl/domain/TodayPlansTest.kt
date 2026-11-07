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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonOverview
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.home.impl.domain.model.LessonBlock
import com.gyugle.gil.feature.home.impl.domain.model.NextStep
import com.gyugle.gil.feature.home.impl.domain.model.ReviewBlock
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import org.junit.jupiter.api.Test

class TodayPlansTest {
    private val lesson = LessonOverview(
        id = LessonId("lesson.s1-17.1"),
        title = LocalizedText("나는 학생이에요", "I am a student"),
        unitTitle = LocalizedText("나와 너", "You and me"),
        stepCount = 12,
        newItemCount = 6
    )

    private fun plan(
        goal: DailyGoal = DailyGoal.TWENTY,
        studied: Duration = Duration.ZERO,
        due: Int = 0,
        reviewed: Int = 0,
        introduced: Int = 0,
        next: LessonOverview? = lesson,
        hasRecord: Boolean = true
    ) = planToday(goal, studied, due, reviewed, introduced, next, hasRecord)

    @Test
    fun planToday_noRecord_startsWithTheFirstLesson() {
        val plan = plan(hasRecord = false)

        assertThat(plan.firstDay).isTrue()
        assertThat(plan.review).isNull()
        assertThat(plan.lesson).isEqualTo(LessonBlock(lesson, recommended = true))
        assertThat(plan.next).isEqualTo(NextStep.Lesson(lesson.id))
    }

    @Test
    fun planToday_someReviewsDue_reviewsFirstThenTheLesson() {
        val plan = plan(due = 12)

        assertThat(plan.review).isEqualTo(ReviewBlock(count = 12, minutes = 6))
        assertThat(plan.moreReview).isNull()
        assertThat(plan.lesson?.recommended).isEqualTo(true)
        assertThat(plan.next).isEqualTo(NextStep.Review(12))
        assertThat(plan.reviewHeavy).isFalse()
    }

    @Test
    fun planToday_reviewsDoneToday_nextIsTheLesson() {
        val plan = plan(due = 0, reviewed = 12)

        assertThat(plan.reviewDoneToday).isTrue()
        assertThat(plan.review).isNull()
        assertThat(plan.next).isEqualTo(NextStep.Lesson(lesson.id))
    }

    @Test
    fun planToday_reviewsOverHalfTheGoal_capsTheFirstBlockAndHoldsBackNewWords() {
        // Ten minutes: five for reviews, ten cards.
        val plan = plan(goal = DailyGoal.TEN, due = 25)

        assertThat(plan.review).isEqualTo(ReviewBlock(count = 10, minutes = 5))
        assertThat(plan.moreReview).isEqualTo(ReviewBlock(count = 15, minutes = 8))
        assertThat(plan.reviewHeavy).isTrue()
        assertThat(plan.lesson?.recommended).isEqualTo(false)
        assertThat(plan.next).isEqualTo(NextStep.Review(10))
    }

    @Test
    fun planToday_reviewShareUsedUpAndLessonHeldBack_goesOnWithTheRestOfTheReviews() {
        val plan = plan(goal = DailyGoal.TEN, due = 15, reviewed = 10)

        assertThat(plan.review).isNull()
        assertThat(plan.moreReview).isEqualTo(ReviewBlock(count = 15, minutes = 8))
        assertThat(plan.next).isEqualTo(NextStep.Review(15))
    }

    @Test
    fun planToday_reviewShareUsedUpAndLessonFits_lessonBeforeTheRest() {
        // Thirty minutes: thirty cards, then two over the share leave room for 16 new items.
        val plan = plan(goal = DailyGoal.THIRTY, due = 2, reviewed = 30)

        assertThat(plan.review).isNull()
        assertThat(plan.lesson?.recommended).isEqualTo(true)
        assertThat(plan.next).isEqualTo(NextStep.Lesson(lesson.id))
        assertThat(plan.moreReview).isEqualTo(ReviewBlock(count = 2, minutes = 1))
    }

    @Test
    fun planToday_aFewCardsOverTheShare_stillRecommendsTheLesson() {
        val plan = plan(goal = DailyGoal.THIRTY, due = 34)

        assertThat(plan.reviewHeavy).isTrue()
        assertThat(plan.lesson?.recommended).isEqualTo(true)
    }

    @Test
    fun planToday_newItemsForTodayUsedUp_lessonOpensButIsNotLed() {
        // Twenty minutes allow twelve new items; eight are learned and the lesson brings six.
        val plan = plan(introduced = 8)

        assertThat(plan.lesson?.recommended).isEqualTo(false)
        assertThat(plan.next).isEqualTo(NextStep.Lesson(lesson.id))
    }

    @Test
    fun planToday_newItemsLeftForAnotherLesson_recommendsIt() {
        assertThat(plan(introduced = 6).lesson?.recommended).isEqualTo(true)
    }

    @Test
    fun planToday_goalMet_keepsTheDueReviewsAndTheLesson() {
        val plan = plan(studied = 20.minutes, due = 3)

        assertThat(plan.goalMet).isTrue()
        assertThat(plan.next).isEqualTo(NextStep.Review(3))
        assertThat(plan.lesson?.recommended).isEqualTo(true)
    }

    @Test
    fun planToday_justUnderTheGoal_notMet() {
        assertThat(plan(studied = 19.minutes).goalMet).isFalse()
    }

    @Test
    fun planToday_courseDoneAndNothingDue_nothingNext() {
        val plan = plan(next = null)

        assertThat(plan.courseDone).isTrue()
        assertThat(plan.next).isNull()
    }

    @Test
    fun planToday_courseDoneWithReviewsDue_reviewsStill() {
        val plan = plan(next = null, goal = DailyGoal.TEN, due = 30)

        assertThat(plan.next).isEqualTo(NextStep.Review(10))
    }

    @Test
    fun reviewedOnAndIntroducedOn_mixedCards_countTodaysByTheirRatings() {
        val today = StudyDay(100)
        fun card(name: String, last: StudyDay, reviews: Int) =
            CardState(SenseId("sense.$name.n.x"), 3.0, last + 3, last, reviews, 0)
        val cards = listOf(
            card("new", today, reviews = 1),
            card("again", today, reviews = 4),
            card("earlier", today - 1, reviews = 2),
            card("yesterday", today - 1, reviews = 1)
        )

        assertThat(reviewedOn(today, cards)).isEqualTo(1)
        assertThat(introducedOn(today, cards)).isEqualTo(1)
    }
}
