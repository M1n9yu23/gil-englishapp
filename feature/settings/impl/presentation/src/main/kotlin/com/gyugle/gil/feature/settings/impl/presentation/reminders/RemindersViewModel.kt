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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.feature.reminder.api.ReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderObserver
import com.gyugle.gil.feature.reminder.api.ReminderTime
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Each change is saved at once. Times are saved even without the notification permission, as the
 * reminder contract allows; the screen then says that nothing will show and offers to allow it.
 * Turning reminders on asks for the permission once, right after the first time is added.
 */
@HiltViewModel
class RemindersViewModel @Inject constructor(
    reminderObserver: ReminderObserver,
    private val reminderCommands: ReminderCommands
) : ViewModel() {

    private val _state = MutableStateFlow(RemindersState())
    val state: StateFlow<RemindersState> = _state.asStateFlow()

    private val eventChannel = Channel<RemindersEvent>()
    val events: Flow<RemindersEvent> = eventChannel.receiveAsFlow()

    init {
        reminderObserver.observeTimes().onEach { times ->
            _state.update { it.copy(times = times) }
        }.launchIn(viewModelScope)
    }

    fun onAction(action: RemindersAction) {
        when (action) {
            is RemindersAction.OnTimeClick -> _state.update { it.copy(editing = action.index) }

            RemindersAction.OnAddClick -> if (_state.value.canAddTime) {
                _state.update { it.copy(editing = RemindersState.NEW_TIME) }
            }

            is RemindersAction.OnRemoveClick -> save { times -> times.filterIndexed { i, _ -> i != action.index } }

            is RemindersAction.OnTimePick -> pick(action.time)

            RemindersAction.OnPickerDismiss -> _state.update { it.copy(editing = null) }

            RemindersAction.OnAllowClick -> send(
                if (_state.value.notifications == PermissionState.PERMANENTLY_DENIED) {
                    RemindersEvent.OpenAppSettings
                } else {
                    RemindersEvent.RequestPermission
                }
            )

            is RemindersAction.OnNotificationsChecked -> _state.update { it.copy(notifications = action.state) }
        }
    }

    private fun pick(time: ReminderTime) {
        val editing = _state.value.editing ?: return
        _state.update { it.copy(editing = null) }
        val turnsOn = _state.value.times.isNullOrEmpty()
        save(askAfter = turnsOn) { times ->
            if (editing == RemindersState.NEW_TIME) {
                times + time
            } else {
                times.mapIndexed { i, old -> if (i == editing) time else old }
            }
        }
    }

    /** Keeps each time once and in the order of the day, as the reminder feature saves them. */
    private fun save(askAfter: Boolean = false, transform: (List<ReminderTime>) -> List<ReminderTime>) {
        val current = _state.value.times ?: return
        val times = transform(current).distinct().sorted().take(ReminderTime.MAX_PER_DAY)
        viewModelScope.launch {
            reminderCommands.setTimes(times)
                .onSuccess {
                    _state.update { it.copy(error = null) }
                    val notifications = _state.value.notifications
                    val canAsk =
                        notifications == PermissionState.NOT_DETERMINED || notifications == PermissionState.DENIED
                    if (askAfter && canAsk) eventChannel.send(RemindersEvent.RequestPermission)
                }
                .onFailure { error -> _state.update { it.copy(error = error.toUiText()) } }
        }
    }

    private fun send(event: RemindersEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
