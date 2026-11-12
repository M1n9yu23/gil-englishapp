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
package com.gyugle.gil.feature.curriculum.impl.presentation.path

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.domain.model.Stage
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitSummary
import com.gyugle.gil.feature.curriculum.impl.domain.repository.FakeCourseRepository
import com.gyugle.gil.feature.curriculum.impl.presentation.LocalizedTextUi
import com.gyugle.gil.feature.curriculum.impl.presentation.toContentErrorUi
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PathViewModelTest {
    private val repository = FakeCourseRepository()
    private val progress = FakeProgressObserver()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    /** A stage with one lesson in each of [units], introducing [items] items in all. */
    private fun stage(id: String, vararg units: String, items: Int = 0) = Stage(
        id = StageId(id),
        title = LocalizedText(ko = "$id ko", en = "$id en"),
        canDo = listOf(LocalizedText(ko = "할 수 있어요", en = "You can")),
        units = units.map { UnitSummary(UnitId(it), LocalizedText(ko = "$it ko", en = "$it en"), lessonCount = 3) },
        lessons = units.map { lessonOf(it) },
        itemCount = items
    )

    private fun lessonOf(unit: String) = LessonId(unit.replace("unit.", "lesson.") + ".1")

    private fun viewModel() = PathViewModel(repository, progress)

    @Test
    fun init_nothingFinished_firstStageWithLessonsIsCurrent() = runTest {
        repository.stages = Outcome.Success(
            listOf(stage("stage.s0"), stage("stage.s1", "unit.s1-01"), stage("stage.a1", "unit.a1-01"))
        )

        val state = viewModel().state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isNull()
        assertThat(
            state.stages.map {
                it.place
            }
        ).containsExactly(StagePlace.PASSED, StagePlace.CURRENT, StagePlace.AHEAD)
        assertThat(state.stages[1].units.single()).isEqualTo(
            UnitRowUi(
                UnitId("unit.s1-01"),
                LocalizedTextUi(ko = "unit.s1-01 ko", en = "unit.s1-01 en"),
                lessonCount = 3
            )
        )
        assertThat(state.stages[1].canDo).containsExactly(LocalizedTextUi(ko = "할 수 있어요", en = "You can"))
    }

    @Test
    fun init_noStageHasLessons_firstStageIsCurrent() = runTest {
        repository.stages = Outcome.Success(listOf(stage("stage.s0"), stage("stage.s1")))

        val state = viewModel().state.value

        assertThat(state.stages.map { it.place }).containsExactly(StagePlace.CURRENT, StagePlace.AHEAD)
    }

    @Test
    fun init_whileReading_showsLoading() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.gate = gate

        val viewModel = viewModel()

        assertThat(viewModel.state.value.isLoading).isTrue()
        gate.complete(Unit)
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun init_contentUnavailable_showsRetryableError() = runTest {
        repository.stages = Outcome.Failure(ContentError.UNAVAILABLE)

        val state = viewModel().state.value

        assertThat(state.isLoading).isFalse()
        assertThat(state.error).isEqualTo(ContentError.UNAVAILABLE.toContentErrorUi())
        assertThat(state.error?.canRetry).isEqualTo(true)
    }

    @Test
    fun onRetryClick_afterFailure_clearsErrorAndShowsStages() = runTest {
        repository.stages = Outcome.Failure(ContentError.UNAVAILABLE)
        val viewModel = viewModel()
        repository.stages = Outcome.Success(listOf(stage("stage.s0", "unit.s0-01")))

        viewModel.onAction(PathAction.OnRetryClick)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.stages.single().id).isEqualTo("stage.s0")
    }

    @Test
    fun init_firstStagesFinished_currentIsTheFirstWithLessonsLeft() = runTest {
        repository.stages = Outcome.Success(
            listOf(stage("stage.s0", "unit.s0-01"), stage("stage.s1", "unit.s1-01"), stage("stage.a1", "unit.a1-01"))
        )
        progress.completedLessons.value = setOf(lessonOf("unit.s0-01"), lessonOf("unit.s1-01"))

        val state = viewModel().state.value

        assertThat(state.stages.map { it.place })
            .containsExactly(StagePlace.PASSED, StagePlace.PASSED, StagePlace.CURRENT)
    }

    @Test
    fun lessonFinished_whileOpen_movesTheCurrentStage() = runTest {
        repository.stages = Outcome.Success(listOf(stage("stage.s1", "unit.s1-01"), stage("stage.a1", "unit.a1-01")))
        val viewModel = viewModel()

        progress.completedLessons.value = setOf(lessonOf("unit.s1-01"))

        assertThat(viewModel.state.value.stages.map { it.place })
            .containsExactly(StagePlace.PASSED, StagePlace.CURRENT)
    }

    @Test
    fun init_everyLessonFinished_nextStageToComeIsCurrent() = runTest {
        repository.stages = Outcome.Success(listOf(stage("stage.s1", "unit.s1-01"), stage("stage.a1")))
        progress.completedLessons.value = setOf(lessonOf("unit.s1-01"))

        val state = viewModel().state.value

        assertThat(state.stages.map { it.place }).containsExactly(StagePlace.PASSED, StagePlace.CURRENT)
    }

    @Test
    fun init_stagesWithLessonsLeft_offerATestWhenTheyHaveEnoughItems() = runTest {
        repository.stages = Outcome.Success(
            listOf(
                stage("stage.s0", "unit.s0-01"),
                stage("stage.s1", "unit.s1-01", items = 45),
                stage("stage.a1", "unit.a1-01", items = 12),
                stage("stage.a2", "unit.a2-01", items = 30)
            )
        )
        progress.completedLessons.value = setOf(lessonOf("unit.a2-01"))

        val state = viewModel().state.value

        // No items in s0, and nothing left to skip in a2.
        assertThat(state.stages.map { it.testQuestions }).containsExactly(null, 20, 12, null)
    }
}
