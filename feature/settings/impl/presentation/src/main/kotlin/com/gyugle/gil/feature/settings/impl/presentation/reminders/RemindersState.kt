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

import androidx.compose.runtime.Immutable
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.feature.reminder.api.ReminderTime

/**
 * @property times null until read.
 * @property editing the index of the time in the picker, or [NEW_TIME] for an added one.
 * @property notifications whether the app may show notifications, null until the screen checks.
 */
@Immutable
data class RemindersState(
    val times: List<ReminderTime>? = null,
    val editing: Int? = null,
    val notifications: PermissionState? = null,
    val error: UiText? = null
) {
    val canAddTime: Boolean get() = times != null && times.size < ReminderTime.MAX_PER_DAY

    /** Times are set but nothing will show them. */
    val isBlocked: Boolean
        get() = !times.isNullOrEmpty() && notifications != null && notifications != PermissionState.GRANTED

    companion object {
        const val NEW_TIME = -1
    }
}

sealed interface RemindersAction {
    data class OnTimeClick(val index: Int) : RemindersAction

    data object OnAddClick : RemindersAction

    data class OnRemoveClick(val index: Int) : RemindersAction

    data class OnTimePick(val time: ReminderTime) : RemindersAction

    data object OnPickerDismiss : RemindersAction

    data object OnAllowClick : RemindersAction

    /** The state on resume, or the answer of the system dialog. */
    data class OnNotificationsChecked(val state: PermissionState) : RemindersAction
}

sealed interface RemindersEvent {
    data object RequestPermission : RemindersEvent

    data object OpenAppSettings : RemindersEvent
}
