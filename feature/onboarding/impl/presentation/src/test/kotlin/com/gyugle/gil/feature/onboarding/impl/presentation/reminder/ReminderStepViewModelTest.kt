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
package com.gyugle.gil.feature.onboarding.impl.presentation.reminder

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.feature.reminder.api.FakeReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderTime
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ReminderStepViewModelTest {
    private val commands = FakeReminderCommands()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        ReminderStepViewModel(handle, commands, commands)

    private fun ReminderStepViewModel.times() = state.value.times

    /** Where a permission result can arrive: the rationale, after "Continue". */
    private fun ReminderStepViewModel.asking() = apply {
        onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))
        onAction(ReminderStepAction.OnAllowClick)
    }

    @Test
    fun init_suggestsOneEveningTime() {
        assertThat(viewModel().times()).containsExactly(ReminderTime(20, 0))
    }

    @Test
    fun addTime_picked_keepsTheDayOrder() {
        val viewModel = viewModel()

        viewModel.onAction(ReminderStepAction.OnAddTimeClick)
        viewModel.onAction(ReminderStepAction.OnTimePicked(ReminderTime(7, 30)))

        assertThat(viewModel.times()).containsExactly(ReminderTime(7, 30), ReminderTime(20, 0))
        assertThat(viewModel.state.value.editing).isNull()
        assertThat(viewModel.state.value.canAddTime).isFalse()
    }

    @Test
    fun addTime_atTheLimit_opensNoPicker() {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnAddTimeClick)
        viewModel.onAction(ReminderStepAction.OnTimePicked(ReminderTime(7, 30)))

        viewModel.onAction(ReminderStepAction.OnAddTimeClick)

        assertThat(viewModel.state.value.editing).isNull()
    }

    @Test
    fun changeTime_toTheOtherTime_keepsItOnce() {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnAddTimeClick)
        viewModel.onAction(ReminderStepAction.OnTimePicked(ReminderTime(7, 30)))

        viewModel.onAction(ReminderStepAction.OnTimeClick(index = 1))
        viewModel.onAction(ReminderStepAction.OnTimePicked(ReminderTime(7, 30)))

        assertThat(viewModel.times()).containsExactly(ReminderTime(7, 30))
    }

    @Test
    fun removeTime_last_leavesNothingToTurnOn() {
        val viewModel = viewModel()

        viewModel.onAction(ReminderStepAction.OnRemoveTimeClick(index = 0))

        assertThat(viewModel.times()).isEmpty()
    }

    @Test
    fun pickerDismissed_changesNothing() {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnTimeClick(index = 0))

        viewModel.onAction(ReminderStepAction.OnPickerDismiss)

        assertThat(viewModel.times()).containsExactly(ReminderTime(20, 0))
        assertThat(viewModel.state.value.editing).isNull()
    }

    @Test
    fun enable_permissionAlreadyGranted_savesAndMovesOn() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnEnableClick)
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.CheckPermission)
            viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.GRANTED))
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.Done)
        }
        assertThat(commands.saved).containsExactly(listOf(ReminderTime(20, 0)))
    }

    @Test
    fun enable_notAskedYet_explainsBeforeTheSystemDialog() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Rationale)
        assertThat(commands.saved).isEmpty()
        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnAllowClick)
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.RequestPermission)
        }
    }

    @Test
    fun permissionGranted_afterTheDialog_saves() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))

        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnPermissionResult(PermissionState.GRANTED))
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.Done)
        }
        assertThat(commands.saved).containsExactly(listOf(ReminderTime(20, 0)))
    }

    @Test
    fun permissionDenied_savesNoTimes() {
        val viewModel = viewModel().asking()

        viewModel.onAction(ReminderStepAction.OnPermissionResult(PermissionState.DENIED))

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Denied(permanently = false))
        assertThat(commands.saved).isEmpty()
    }

    @Test
    fun permissionDeniedForGood_offersSettings_andSavesOnceAllowedThere() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.asking()
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.RequestPermission)
            viewModel.onAction(ReminderStepAction.OnPermissionResult(PermissionState.PERMANENTLY_DENIED))
            assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Denied(permanently = true))
            viewModel.onAction(ReminderStepAction.OnOpenSettingsClick)
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.OpenAppSettings)
            viewModel.onAction(ReminderStepAction.OnResumed(PermissionState.GRANTED))
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.Done)
        }
        assertThat(commands.saved).containsExactly(listOf(ReminderTime(20, 0)))
    }

    @Test
    fun resumed_whilePickingTimes_savesNothing() {
        val viewModel = viewModel()

        viewModel.onAction(ReminderStepAction.OnResumed(PermissionState.GRANTED))

        assertThat(commands.saved).isEmpty()
    }

    @Test
    fun skip_withTimes_turnsRemindersOffAndMovesOn() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnSkipClick)
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.Done)
        }
        assertThat(commands.saved).containsExactly(emptyList<ReminderTime>())
    }

    @Test
    fun save_writeFails_staysWithTheError() = runTest {
        commands.error = DataError.Local.UNKNOWN
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.GRANTED))
            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isSaving).isFalse()
    }

    @Test
    fun init_timesSavedOnAnEarlierVisit_showsThem() {
        commands.times.value = listOf(ReminderTime(7, 0))

        assertThat(viewModel().times()).containsExactly(ReminderTime(7, 0))
    }

    // Allowed, moved on, then back to this step: the step must stay put for the learner to edit.
    @Test
    fun resumed_afterTheStepIsDone_doesNotMoveOnAgain() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.asking()
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.RequestPermission)
            viewModel.onAction(ReminderStepAction.OnPermissionResult(PermissionState.GRANTED))
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.Done)

            viewModel.onAction(ReminderStepAction.OnResumed(PermissionState.GRANTED))
            expectNoEvents()
        }
        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Times)
        assertThat(commands.saved).hasSize(1)
    }

    @Test
    fun allowClickedTwice_asksOnce() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))

        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnAllowClick)
            viewModel.onAction(ReminderStepAction.OnAllowClick)
            assertThat(awaitItem()).isEqualTo(ReminderStepEvent.RequestPermission)
            expectNoEvents()
        }
    }

    // A rotation while the dialog is up drops its result; the next resume reads the answer.
    @Test
    fun resumed_afterARotationDroppedARefusal_showsTheRefusal() = runTest {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))
        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnAllowClick)
            awaitItem()
        }

        viewModel.onAction(ReminderStepAction.OnResumed(PermissionState.DENIED))

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Denied(permanently = false))
    }

    @Test
    fun result_afterTheResumeGuessed_refinesToPermanent() {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))
        viewModel.onAction(ReminderStepAction.OnAllowClick)
        viewModel.onAction(ReminderStepAction.OnResumed(PermissionState.DENIED))

        viewModel.onAction(ReminderStepAction.OnPermissionResult(PermissionState.PERMANENTLY_DENIED))

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Denied(permanently = true))
    }

    @Test
    fun resumed_onTheRationaleWithoutAsking_staysThere() {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))

        viewModel.onAction(ReminderStepAction.OnResumed(PermissionState.NOT_DETERMINED))

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Rationale)
    }

    @Test
    fun permissionResultNotDetermined_readsAsARefusalTheSystemMayAskAgain() {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))

        viewModel.onAction(ReminderStepAction.OnPermissionResult(PermissionState.NOT_DETERMINED))

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Denied(permanently = false))
    }

    // Below Android 13 the Root reports the app's notifications turned off as a refusal for good.
    @Test
    fun enable_notificationsTurnedOffForTheApp_offersSettings() {
        val viewModel = viewModel()

        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.PERMANENTLY_DENIED))

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Denied(permanently = true))
    }

    @Test
    fun backOnTheRationale_returnsToTheTimes() {
        val viewModel = viewModel()
        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.NOT_DETERMINED))

        viewModel.onAction(ReminderStepAction.OnBackToTimesClick)

        assertThat(viewModel.state.value.stage).isEqualTo(ReminderStage.Times)
        assertThat(viewModel.times()).containsExactly(ReminderTime(20, 0))
    }

    @Test
    fun enable_whileSaving_savesOnce() {
        val saving = CompletableDeferred<Unit>()
        commands.saving = saving
        val viewModel = viewModel()

        viewModel.onAction(ReminderStepAction.OnPermissionChecked(PermissionState.GRANTED))
        assertThat(viewModel.state.value.isSaving).isTrue()
        viewModel.onAction(ReminderStepAction.OnSkipClick)
        saving.complete(Unit)

        assertThat(commands.saved).containsExactly(listOf(ReminderTime(20, 0)))
    }

    @Test
    fun skip_writeFails_staysWithTheError() = runTest {
        commands.error = DataError.Local.UNKNOWN
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(ReminderStepAction.OnSkipClick)
            expectNoEvents()
        }
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun processDeath_keepsTimesAndStage() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            onAction(ReminderStepAction.OnAddTimeClick)
            onAction(ReminderStepAction.OnTimePicked(ReminderTime(6, 45)))
            asking()
            onAction(ReminderStepAction.OnPermissionResult(PermissionState.PERMANENTLY_DENIED))
        }

        val restored = viewModel(handle).state.value

        assertThat(restored.times).containsExactly(ReminderTime(6, 45), ReminderTime(20, 0))
        assertThat(restored.stage).isEqualTo(ReminderStage.Denied(permanently = true))
    }
}
