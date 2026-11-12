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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.lesson.api.LessonRoute
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.review.api.ReviewSessionRoute
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class TodayContinueFinderTest {
    private val first = lesson("lesson.s1-17.1", next = "lesson.s1-17.2")
    private val second = lesson("lesson.s1-17.2", next = null)
    private val course = FakeCourseObserver(first, second)
    private val progress = FakeProgressObserver()
    private val goals = object : GoalObserver {
        override fun observeGoal(): Flow<DailyGoal> = flowOf(DailyGoal.TWENTY)
    }
    private val clock = FakeStudyClock()
    private val today = clock.today()

    private fun finder() = TodayContinueFinder(goals, progress, course, clock)

    private fun lesson(id: String, next: String?) = Lesson(
        id = LessonId(id),
        title = LocalizedText("", ""),
        goal = LocalizedText("", ""),
        steps = emptyList(),
        introduced = listOf(LessonItem(SenseId("sense.hi.interj.greeting"), "hi", "haɪ", "안녕")),
        next = next?.let(::LessonId)
    )

    private fun card(name: String, due: Int) = CardState(
        SenseId("sense.$name.n.x"),
        stabilityDays = 3.0,
        dueDay = today + due,
        lastReviewDay = today - 3,
        reviewCount = 2,
        lapseCount = 0
    )

    @Test
    fun find_noRecord_opensTheFirstLesson() = runTest {
        assertThat(finder().find()).isEqualTo(LessonRoute(first.id))
    }

    @Test
    fun find_reviewsDue_opensTheReviewsTheTodayTabLeadsWith() = runTest {
        progress.completedLessons.value = setOf(first.id)
        progress.cards.value = listOf(card("a", due = 0), card("b", due = -2), card("c", due = 4))

        assertThat(finder().find()).isEqualTo(ReviewSessionRoute(limit = 2))
    }

    @Test
    fun find_nothingDueAndALessonLeft_opensTheNextLesson() = runTest {
        progress.completedLessons.value = setOf(first.id)
        progress.cards.value = listOf(card("a", due = 4))

        assertThat(finder().find()).isEqualTo(LessonRoute(second.id))
    }

    @Test
    fun find_courseDoneAndNothingDue_isNull() = runTest {
        progress.completedLessons.value = setOf(first.id, second.id)

        assertThat(finder().find()).isNull()
    }

    @Test
    fun find_courseUnreadable_isNull() = runTest {
        course.error = ContentError.UNAVAILABLE

        assertThat(finder().find()).isNull()
    }
}
