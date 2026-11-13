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
package com.gyugle.gil.feature.curriculum.impl.presentation.unitdetail

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.domain.model.LessonSummary
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitDetail
import com.gyugle.gil.feature.curriculum.impl.domain.repository.FakeCourseRepository
import com.gyugle.gil.feature.curriculum.impl.presentation.LocalizedTextUi
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class UnitDetailViewModelTest {
    private val repository = FakeCourseRepository()
    private val progress = FakeProgressObserver()
    private val unitId = UnitId("unit.s1-01")

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun text(value: String) = LocalizedText(ko = "$value ko", en = "$value en")

    private fun ui(value: String) = LocalizedTextUi(ko = "$value ko", en = "$value en")

    private val unit = UnitDetail(
        id = unitId,
        stageTitle = text("stage"),
        title = text("unit"),
        lessons = listOf(
            LessonSummary(LessonId("lesson.s1-01.1"), text("first"), text("goal 1")),
            LessonSummary(LessonId("lesson.s1-01.2"), text("second"), text("goal 2"))
        )
    )

    private fun courseLesson(id: String) = Lesson(
        id = LessonId(id),
        title = text(id),
        goal = text("goal"),
        steps = emptyList(),
        introduced = emptyList(),
        next = null
    )

    private val course = FakeCourseObserver(courseLesson("lesson.s1-01.1"), courseLesson("lesson.s1-01.2"))

    private fun viewModel() = UnitDetailViewModel(unitId.value, repository, course, progress)

    @Test
    fun init_knownUnit_showsLessonsNumberedFromOne() = runTest {
        repository.units = mapOf(unitId to unit)

        val state = viewModel().state.value

        assertThat(repository.requestedUnits).containsExactly(unitId)
        assertThat(state.isLoading).isFalse()
        assertThat(state.unit).isEqualTo(
            UnitDetailUi(
                title = ui("unit"),
                stageTitle = ui("stage"),
                lessons = listOf(
                    LessonRowUi(LessonId("lesson.s1-01.1"), number = 1, title = ui("first"), goal = ui("goal 1")),
                    LessonRowUi(LessonId("lesson.s1-01.2"), number = 2, title = ui("second"), goal = ui("goal 2"))
                )
            )
        )
    }

    @Test
    fun init_unknownUnit_showsErrorThatOffersNoRetry() = runTest {
        val state = viewModel().state.value

        assertThat(state.unit).isNull()
        assertThat(state.error?.canRetry).isEqualTo(false)
    }

    @Test
    fun onRetryClick_afterUnavailable_showsUnit() = runTest {
        repository.unitError = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat(viewModel.state.value.error?.canRetry).isEqualTo(true)
        repository.unitError = null
        repository.units = mapOf(unitId to unit)

        viewModel.onAction(UnitDetailAction.OnRetryClick)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.unit?.title).isEqualTo(ui("unit"))
    }

    @Test
    fun state_finishedLesson_isMarkedDoneAsTheRecordChanges() = runTest {
        repository.units = mapOf(unitId to unit)
        val viewModel = viewModel()
        assertThat(viewModel.state.value.unit?.lessons?.map { it.done }).isEqualTo(listOf(false, false))

        progress.completedLessons.value = setOf(LessonId("lesson.s1-01.1"))

        assertThat(viewModel.state.value.unit?.lessons?.map { it.done }).isEqualTo(listOf(true, false))
    }

    @Test
    fun onLessonClick_nextLesson_opensIt() = runTest {
        repository.units = mapOf(unitId to unit)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(UnitDetailAction.OnLessonClick(LessonId("lesson.s1-01.1")))

            assertThat(awaitItem()).isEqualTo(UnitDetailEvent.OpenLesson(LessonId("lesson.s1-01.1")))
        }
        assertThat(viewModel.state.value.skipAhead).isNull()
    }

    @Test
    fun onLessonClick_pastAnUnfinishedLesson_asksOnceThenOpensWhatIsChosen() = runTest {
        repository.units = mapOf(unitId to unit)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(UnitDetailAction.OnLessonClick(LessonId("lesson.s1-01.2")))
            assertThat(viewModel.state.value.skipAhead).isEqualTo(
                SkipAheadUi(LessonId("lesson.s1-01.2"), LessonId("lesson.s1-01.1"), ui("lesson.s1-01.1"))
            )

            viewModel.onAction(UnitDetailAction.OnSkipAheadToEarlier)
            assertThat(awaitItem()).isEqualTo(UnitDetailEvent.OpenLesson(LessonId("lesson.s1-01.1")))
            assertThat(viewModel.state.value.skipAhead).isNull()

            viewModel.onAction(UnitDetailAction.OnLessonClick(LessonId("lesson.s1-01.2")))
            assertThat(awaitItem()).isEqualTo(UnitDetailEvent.OpenLesson(LessonId("lesson.s1-01.2")))
        }
    }

    @Test
    fun onLessonClick_finishedLesson_opensWithoutAsking() = runTest {
        repository.units = mapOf(unitId to unit)
        progress.completedLessons.value = setOf(LessonId("lesson.s1-01.2"))
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(UnitDetailAction.OnLessonClick(LessonId("lesson.s1-01.2")))

            assertThat(awaitItem()).isEqualTo(UnitDetailEvent.OpenLesson(LessonId("lesson.s1-01.2")))
        }
    }

    @Test
    fun onLessonClick_courseOrderUnreadable_opensWhatWasPicked() = runTest {
        repository.units = mapOf(unitId to unit)
        course.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(UnitDetailAction.OnLessonClick(LessonId("lesson.s1-01.2")))

            assertThat(awaitItem()).isEqualTo(UnitDetailEvent.OpenLesson(LessonId("lesson.s1-01.2")))
        }
        assertThat(viewModel.state.value.skipAhead).isNull()
    }
}
