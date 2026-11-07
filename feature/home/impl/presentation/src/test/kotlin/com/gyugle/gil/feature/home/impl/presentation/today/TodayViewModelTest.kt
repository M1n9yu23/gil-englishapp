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
package com.gyugle.gil.feature.home.impl.presentation.today

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.home.impl.domain.model.NextStep
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TodayViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val first = lesson("lesson.s1-17.1", next = "lesson.s1-17.2")
    private val second = lesson("lesson.s1-17.2", next = null)
    private val course = FakeCourseObserver(first, second)
    private val progress = FakeProgressObserver()
    private val goal = MutableStateFlow(DailyGoal.TWENTY)
    private val goals = object : GoalObserver {
        override fun observeGoal(): Flow<DailyGoal> = goal
    }
    private val clock = FakeStudyClock()
    private val today = clock.today()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun lesson(id: String, next: String?) = Lesson(
        id = LessonId(id),
        title = LocalizedText("$id 제목", "$id title"),
        goal = LocalizedText("목표", "Goal"),
        steps = List(10) { LessonStep.Listen(Sentence(it.toLong(), "Hi.", "안녕.")) },
        introduced = listOf(LessonItem(SenseId("sense.hi.interj.greeting"), "hi", "haɪ", "안녕")),
        next = next?.let(::LessonId)
    )

    private fun viewModel() = TodayViewModel(goals, progress, course, clock)

    private fun card(name: String, due: Int, last: Int = -3, reviews: Int = 2) = CardState(
        SenseId("sense.$name.n.x"),
        stabilityDays = 3.0,
        dueDay = today + due,
        lastReviewDay = today + last,
        reviewCount = reviews,
        lapseCount = 0
    )

    /** The next plan, past the loading state. */
    private suspend fun ReceiveTurbine<TodayState>.ready(): TodayUi {
        var state = awaitItem()
        while (state is TodayState.Loading) state = awaitItem()
        return (state as TodayState.Ready).today
    }

    /**
     * The first plan that passes [check]. The record's flows each emit on a change, so a plan made
     * from one new flow and one old one can come first.
     */
    private suspend fun ReceiveTurbine<TodayState>.readyWhen(check: (TodayUi) -> Boolean): TodayUi {
        var today = ready()
        while (!check(today)) today = ready()
        return today
    }

    @Test
    fun state_noRecord_leadsToTheFirstLesson() = runTest(dispatcher) {
        viewModel().state.test {
            val today = ready()

            assertThat(today.message).isEqualTo(TodayMessage.FIRST_DAY)
            assertThat(today.firstLesson).isEqualTo(true)
            assertThat(today.next).isEqualTo(NextStep.Lesson(first.id))
            assertThat(today.blocks).containsExactly(
                BlockUi.Lesson(
                    id = first.id,
                    title = LocalizedTextUi("lesson.s1-17.1 제목", "lesson.s1-17.1 title"),
                    unitTitle = LocalizedTextUi("단원", "Unit"),
                    minutes = 10,
                    enoughNewWords = false
                )
            )
        }
    }

    @Test
    fun state_reviewsDue_reviewsComeFirst() = runTest(dispatcher) {
        progress.completedLessons.value = setOf(first.id)
        progress.cards.value = listOf(card("a", due = 0), card("b", due = -2), card("c", due = 4))

        viewModel().state.test {
            val today = ready()

            assertThat(today.message).isEqualTo(null)
            assertThat(today.next).isEqualTo(NextStep.Review(2))
            assertThat(today.blocks.first()).isEqualTo(BlockUi.Review(count = 2, minutes = 1, more = false))
            assertThat((today.blocks[1] as BlockUi.Lesson).id).isEqualTo(second.id)
        }
    }

    @Test
    fun state_recordChanges_planFollows() = runTest(dispatcher) {
        progress.completedLessons.value = setOf(first.id)
        progress.cards.value = listOf(card("a", due = 0))

        viewModel().state.test {
            assertThat(ready().next).isEqualTo(NextStep.Review(1))

            progress.cards.value = listOf(card("a", due = 3, last = 0, reviews = 3))

            val today = readyWhen { it.blocks.first() == BlockUi.ReviewDone }
            assertThat(today.next).isEqualTo(NextStep.Lesson(second.id))
        }
    }

    @Test
    fun state_studyTime_fillsTheGoalAndTheWeek() = runTest(dispatcher) {
        progress.completedLessons.value = setOf(first.id)
        progress.cards.value = listOf(card("a", due = 5))
        progress.studyTime.value = listOf(DailyStudyTime(today, 21.minutes), DailyStudyTime(today - 1, 3.minutes))
        progress.streak.value = 4

        viewModel().state.test {
            val today = ready()

            assertThat(today.studiedMinutes).isEqualTo(21)
            assertThat(today.goalMinutes).isEqualTo(20)
            assertThat(today.message).isEqualTo(TodayMessage.GOAL_MET)
            assertThat(today.streak).isEqualTo(4)
            assertThat(today.week.single { it.isToday }.studied).isEqualTo(true)
            assertThat(today.week.count { it.studied }).isEqualTo(1)
        }
    }

    @Test
    fun state_newStudyDay_movesThePlanOn() = runTest(dispatcher) {
        progress.completedLessons.value = setOf(first.id)
        progress.cards.value = listOf(card("a", due = 1))

        viewModel().state.test {
            assertThat(ready().next).isEqualTo(NextStep.Lesson(second.id))

            clock.advanceBy(1.days)
            advanceTimeBy(1.minutes)

            assertThat(readyWhen { it.next is NextStep.Review }.next).isEqualTo(NextStep.Review(1))
        }
    }

    @Test
    fun state_everyLessonDone_saysSoAndOffersNothingNew() = runTest(dispatcher) {
        progress.completedLessons.value = setOf(first.id, second.id)
        progress.cards.value = listOf(card("a", due = 2))

        viewModel().state.test {
            val today = ready()

            assertThat(today.message).isEqualTo(null)
            assertThat(today.next).isEqualTo(null)
            assertThat(today.blocks).containsExactly(BlockUi.CourseDone)
        }
    }

    @Test
    fun state_contentUnavailable_failsAndRetryRecovers() = runTest(dispatcher) {
        course.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()

        viewModel.state.test {
            var state = awaitItem()
            while (state is TodayState.Loading) state = awaitItem()
            assertThat(state).isEqualTo(TodayState.Failed)

            course.error = null
            viewModel.onAction(TodayAction.OnRetryClick)

            assertThat(ready().next).isEqualTo(NextStep.Lesson(first.id))
        }
    }

    @Test
    fun continue_opensWhatThePlanLeadsWith() = runTest(dispatcher) {
        progress.completedLessons.value = setOf(first.id)
        progress.cards.value = listOf(card("a", due = 0), card("b", due = 0))
        val viewModel = viewModel()

        viewModel.state.test {
            ready()
            viewModel.events.test {
                viewModel.onAction(TodayAction.OnContinueClick)

                assertThat(awaitItem()).isEqualTo(TodayEvent.OpenReview(2))
            }
        }
    }

    @Test
    fun blockClick_opensThatBlock() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.state.test {
            val lessonBlock = ready().blocks.single()
            viewModel.events.test {
                viewModel.onAction(TodayAction.OnBlockClick(lessonBlock))

                assertThat(awaitItem()).isInstanceOf<TodayEvent.OpenLesson>()
            }
        }
    }
}
