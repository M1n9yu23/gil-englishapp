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
package com.gyugle.gil.feature.onboarding.impl.presentation.welcome

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.FakeGoalRepository
import com.gyugle.gil.feature.onboarding.impl.domain.repository.FakeOnboardingRepository
import com.gyugle.gil.feature.reminder.api.FakeReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WelcomeViewModelTest {
    private val onboarding = FakeOnboardingRepository()
    private val goals = FakeGoalRepository()
    private val reminders = FakeReminderCommands()

    private fun viewModel() = WelcomeViewModel(onboarding, goals, reminders)

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    @Test
    fun skip_firstVisit_endsTheFirstRun() {
        viewModel().onAction(WelcomeAction.OnSkipClick)

        assertThat(onboarding.completed.value).isTrue()
    }

    // Back from later steps with a goal and reminders saved: skipping still means the defaults.
    @Test
    fun skip_afterSavingLaterSteps_restoresTheDefaults() {
        goals.goal.value = DailyGoal.SIXTY
        reminders.times.value = listOf(ReminderTime(20, 0))

        viewModel().onAction(WelcomeAction.OnSkipClick)

        assertThat(goals.goal.value).isEqualTo(DailyGoal.TWENTY)
        assertThat(reminders.times.value).isEmpty()
        assertThat(onboarding.completed.value).isTrue()
    }

    @Test
    fun skip_writeFails_showsTheErrorAndCanTryAgain() {
        onboarding.error = DataError.Local.DISK_FULL
        val viewModel = viewModel()

        viewModel.onAction(WelcomeAction.OnSkipClick)

        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isFinishing).isFalse()
        onboarding.error = null
        viewModel.onAction(WelcomeAction.OnSkipClick)
        assertThat(onboarding.completeCount).isEqualTo(2)
    }
}
