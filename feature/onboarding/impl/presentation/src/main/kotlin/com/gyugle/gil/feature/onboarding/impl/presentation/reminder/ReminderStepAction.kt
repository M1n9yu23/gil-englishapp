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

import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.feature.reminder.api.ReminderTime

sealed interface ReminderStepAction {
    data class OnTimeClick(val index: Int) : ReminderStepAction

    data object OnAddTimeClick : ReminderStepAction

    data class OnRemoveTimeClick(val index: Int) : ReminderStepAction

    data class OnTimePicked(val time: ReminderTime) : ReminderStepAction

    data object OnPickerDismiss : ReminderStepAction

    data object OnEnableClick : ReminderStepAction

    /** The Root's answer to [ReminderStepEvent.CheckPermission]. */
    data class OnPermissionChecked(val state: PermissionState) : ReminderStepAction

    /** "Continue" on the rationale: show the system dialog. */
    data object OnAllowClick : ReminderStepAction

    /** The Root's answer to [ReminderStepEvent.RequestPermission]. */
    data class OnPermissionResult(val state: PermissionState) : ReminderStepAction

    /** The screen is back, perhaps from the app's system settings where the learner allowed it. */
    data class OnResumed(val state: PermissionState) : ReminderStepAction

    data object OnOpenSettingsClick : ReminderStepAction

    /** Back from the rationale or the refusal: to the times, not out of the step. */
    data object OnBackToTimesClick : ReminderStepAction

    /** Goes on with reminders off, from any stage. */
    data object OnSkipClick : ReminderStepAction
}
