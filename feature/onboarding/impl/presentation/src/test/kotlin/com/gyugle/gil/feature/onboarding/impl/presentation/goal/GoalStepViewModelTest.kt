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
package com.gyugle.gil.feature.onboarding.impl.presentation.goal

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.FakeGoalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GoalStepViewModelTest {
    private val goals = FakeGoalRepository()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) = GoalStepViewModel(handle, goals, goals)

    @Test
    fun init_firstVisit_selectsTheDefault() {
        assertThat(viewModel().state.value.selected).isEqualTo(DailyGoal.TWENTY)
    }

    @Test
    fun init_comingBack_selectsTheSavedGoal() {
        goals.goal.value = DailyGoal.SIXTY

        assertThat(viewModel().state.value.selected).isEqualTo(DailyGoal.SIXTY)
    }

    @Test
    fun onGoalSelect_beforeNext_savesNothing() {
        val viewModel = viewModel()

        viewModel.onAction(GoalStepAction.OnGoalSelect(DailyGoal.TEN))

        assertThat(viewModel.state.value.selected).isEqualTo(DailyGoal.TEN)
        assertThat(goals.goal.value).isEqualTo(DailyGoal.TWENTY)
    }

    @Test
    fun onNextClick_savesTheChoiceAndMovesOn() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(GoalStepAction.OnGoalSelect(DailyGoal.THIRTY))

        viewModel.events.test {
            viewModel.onAction(GoalStepAction.OnNextClick)
            assertThat(awaitItem()).isEqualTo(GoalStepEvent.Saved)
        }
        assertThat(goals.goal.value).isEqualTo(DailyGoal.THIRTY)
    }

    @Test
    fun onNextClick_writeFails_staysWithTheError() = runTest {
        goals.error = DataError.Local.DISK_FULL
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(GoalStepAction.OnNextClick)
            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isSaving).isFalse()
    }

    @Test
    fun processDeath_keepsTheUnsavedChoiceOverTheSavedGoal() {
        val handle = SavedStateHandle()
        viewModel(handle).onAction(GoalStepAction.OnGoalSelect(DailyGoal.TEN))
        goals.goal.value = DailyGoal.SIXTY

        assertThat(viewModel(handle).state.value.selected).isEqualTo(DailyGoal.TEN)
    }
}
