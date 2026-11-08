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
package com.gyugle.gil.feature.settings.impl.presentation.reminders

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.feature.reminder.api.FakeReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class RemindersViewModelTest {
    private val reminders = FakeReminderCommands()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = RemindersViewModel(reminders, reminders)

    @Test
    fun addTime_firstOne_savesItAndAsksForNotifications() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(RemindersAction.OnNotificationsChecked(PermissionState.NOT_DETERMINED))

        viewModel.events.test {
            viewModel.onAction(RemindersAction.OnAddClick)
            viewModel.onAction(RemindersAction.OnTimePick(ReminderTime(7, 30)))

            assertThat(awaitItem()).isEqualTo(RemindersEvent.RequestPermission)
        }
        assertThat(reminders.saved.single()).containsExactly(ReminderTime(7, 30))
        assertThat(viewModel.state.value.editing).isNull()
    }

    @Test
    fun addTime_alreadyAllowed_doesNotAsk() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(RemindersAction.OnNotificationsChecked(PermissionState.GRANTED))

        viewModel.events.test {
            viewModel.onAction(RemindersAction.OnAddClick)
            viewModel.onAction(RemindersAction.OnTimePick(ReminderTime(7, 30)))
            expectNoEvents()
        }
    }

    @Test
    fun times_keptInDayOrderAndAtMostTwo() {
        reminders.times.value = listOf(ReminderTime(20, 0))
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.OnAddClick)
        viewModel.onAction(RemindersAction.OnTimePick(ReminderTime(8, 0)))

        assertThat(reminders.times.value).containsExactly(ReminderTime(8, 0), ReminderTime(20, 0))
        assertThat(viewModel.state.value.canAddTime).isEqualTo(false)
        viewModel.onAction(RemindersAction.OnAddClick)
        assertThat(viewModel.state.value.editing).isNull()
    }

    @Test
    fun changeTime_replacesThatTime() {
        reminders.times.value = listOf(ReminderTime(8, 0), ReminderTime(20, 0))
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.OnTimeClick(1))
        viewModel.onAction(RemindersAction.OnTimePick(ReminderTime(21, 15)))

        assertThat(reminders.times.value).containsExactly(ReminderTime(8, 0), ReminderTime(21, 15))
    }

    @Test
    fun removeLastTime_turnsRemindersOff() {
        reminders.times.value = listOf(ReminderTime(20, 0))
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.OnRemoveClick(0))

        assertThat(reminders.times.value).isEmpty()
    }

    @Test
    fun blocked_onlyWhenTimesAreSetAndNotificationsAreNotAllowed() {
        reminders.times.value = listOf(ReminderTime(20, 0))
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.OnNotificationsChecked(PermissionState.DENIED))
        assertThat(viewModel.state.value.isBlocked).isEqualTo(true)

        viewModel.onAction(RemindersAction.OnNotificationsChecked(PermissionState.GRANTED))
        assertThat(viewModel.state.value.isBlocked).isEqualTo(false)

        reminders.times.value = emptyList()
        viewModel.onAction(RemindersAction.OnNotificationsChecked(PermissionState.DENIED))
        assertThat(viewModel.state.value.isBlocked).isEqualTo(false)
    }

    @Test
    fun allow_permanentlyDenied_opensTheAppSettings() = runTest {
        reminders.times.value = listOf(ReminderTime(20, 0))
        val viewModel = viewModel()
        viewModel.onAction(RemindersAction.OnNotificationsChecked(PermissionState.PERMANENTLY_DENIED))

        viewModel.events.test {
            viewModel.onAction(RemindersAction.OnAllowClick)
            assertThat(awaitItem()).isEqualTo(RemindersEvent.OpenAppSettings)
        }
    }

    @Test
    fun saveFails_showsTheErrorAndKeepsTheTimes() {
        reminders.times.value = listOf(ReminderTime(20, 0))
        reminders.error = DataError.Local.UNKNOWN
        val viewModel = viewModel()

        viewModel.onAction(RemindersAction.OnRemoveClick(0))

        assertThat(viewModel.state.value.error).isEqualTo(DataError.Local.UNKNOWN.toUiText())
        assertThat(viewModel.state.value.times).isEqualTo(listOf(ReminderTime(20, 0)))
    }
}
