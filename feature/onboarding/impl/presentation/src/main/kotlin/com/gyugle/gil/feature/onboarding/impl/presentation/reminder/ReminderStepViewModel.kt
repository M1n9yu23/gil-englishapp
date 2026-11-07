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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Times are saved only once notifications are allowed, so a refusal leaves reminders off instead
 * of scheduling reminders nobody sees. On Android 12 and lower there is no permission to ask, and
 * the Root reports notifications turned off for the app as a refusal for good.
 *
 * The times and the stage survive process death; an open time picker does not. A save goes back
 * to the times, so returning to this step later shows them instead of moving on again.
 */
@HiltViewModel
class ReminderStepViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val reminderObserver: ReminderObserver,
    private val reminderCommands: ReminderCommands
) : ViewModel() {

    private val _state = MutableStateFlow(
        ReminderStepState(
            times = savedStateHandle.get<IntArray>(KEY_TIMES)?.map(::timeOfMinutes) ?: listOf(FIRST_SUGGESTION),
            stage = savedStateHandle.get<String>(KEY_STAGE)?.let(::stageOf) ?: ReminderStage.Times
        )
    )
    val state: StateFlow<ReminderStepState> = _state.asStateFlow()

    private val eventChannel = Channel<ReminderStepEvent>()
    val events: Flow<ReminderStepEvent> = eventChannel.receiveAsFlow()

    /**
     * The system dialog is up or about to be. A second tap must not ask again: two refusals in a
     * row make the refusal permanent. Cleared by the result or, when a rotation dropped it, by the
     * next resume.
     */
    private var awaitingPermission = false

    init {
        // Coming back to this step shows the times saved before, unless the learner changed them.
        if (KEY_TIMES !in savedStateHandle) {
            viewModelScope.launch {
                val saved = reminderObserver.observeTimes().first()
                if (saved.isNotEmpty() && KEY_TIMES !in savedStateHandle) updateTimes { saved }
            }
        }
    }

    fun onAction(action: ReminderStepAction) {
        when (action) {
            is ReminderStepAction.OnTimeClick -> _state.update { it.copy(editing = action.index) }
            ReminderStepAction.OnAddTimeClick -> addTime()
            is ReminderStepAction.OnRemoveTimeClick -> removeTime(action.index)
            is ReminderStepAction.OnTimePicked -> pick(action.time)
            ReminderStepAction.OnPickerDismiss -> _state.update { it.copy(editing = null) }
            ReminderStepAction.OnEnableClick -> send(ReminderStepEvent.CheckPermission)
            is ReminderStepAction.OnPermissionChecked -> onPermissionChecked(action.state)
            ReminderStepAction.OnAllowClick -> requestPermission()
            is ReminderStepAction.OnPermissionResult -> onPermissionResult(action.state)
            is ReminderStepAction.OnResumed -> onResumed(action.state)
            ReminderStepAction.OnOpenSettingsClick -> send(ReminderStepEvent.OpenAppSettings)
            ReminderStepAction.OnBackToTimesClick -> setStage(ReminderStage.Times)
            ReminderStepAction.OnSkipClick -> turnOff()
        }
    }

    private fun addTime() {
        if (_state.value.canAddTime) _state.update { it.copy(editing = ReminderStepState.NEW_TIME) }
    }

    private fun removeTime(index: Int) = updateTimes { times -> times.filterIndexed { i, _ -> i != index } }

    private fun pick(time: ReminderTime) {
        val editing = _state.value.editing ?: return
        _state.update { it.copy(editing = null) }
        updateTimes { times ->
            if (editing ==
                ReminderStepState.NEW_TIME
            ) {
                times + time
            } else {
                times.replaced(editing, time)
            }
        }
    }

    private fun List<ReminderTime>.replaced(index: Int, time: ReminderTime) =
        mapIndexed { i, old -> if (i == index) time else old }

    /** Keeps each time once and in the order of the day, as they will be saved. */
    private fun updateTimes(transform: (List<ReminderTime>) -> List<ReminderTime>) {
        val times = transform(_state.value.times).distinct().sorted().take(ReminderTime.MAX_PER_DAY)
        savedStateHandle[KEY_TIMES] = times.map { it.hour * MINUTES_PER_HOUR + it.minute }.toIntArray()
        _state.update { it.copy(times = times, error = null) }
    }

    private fun onPermissionChecked(state: PermissionState) {
        when (state) {
            PermissionState.GRANTED -> save()

            // Only from Android 12 and lower, where the app's notifications are turned off.
            PermissionState.PERMANENTLY_DENIED -> setStage(ReminderStage.Denied(permanently = true))

            PermissionState.DENIED, PermissionState.NOT_DETERMINED -> setStage(ReminderStage.Rationale)
        }
    }

    private fun requestPermission() {
        if (awaitingPermission) return
        awaitingPermission = true
        send(ReminderStepEvent.RequestPermission)
    }

    /** Taken while asking, or after a resume guessed the answer and the result refines it. */
    private fun onPermissionResult(state: PermissionState) {
        awaitingPermission = false
        val stage = _state.value.stage
        if (stage != ReminderStage.Rationale && stage != DENIED_FOR_NOW) return
        when (state) {
            PermissionState.GRANTED -> save()

            PermissionState.PERMANENTLY_DENIED -> setStage(ReminderStage.Denied(permanently = true))

            // A request never answers "not asked"; treat it like a refusal the system may ask again.
            PermissionState.DENIED, PermissionState.NOT_DETERMINED -> setStage(DENIED_FOR_NOW)
        }
    }

    /**
     * Back from the system dialog, the app's settings, or a rotation that dropped the dialog's
     * result. Only a change to "allowed" moves on; picking times is never interrupted.
     */
    private fun onResumed(state: PermissionState) {
        val waited = awaitingPermission
        awaitingPermission = false
        val stage = _state.value.stage
        if (stage == ReminderStage.Times) return
        if (state == PermissionState.GRANTED) {
            save()
        } else if (waited && state == PermissionState.DENIED && stage == ReminderStage.Rationale) {
            setStage(DENIED_FOR_NOW)
        }
    }

    private fun setStage(stage: ReminderStage) {
        savedStateHandle[KEY_STAGE] = stage.key()
        _state.update { it.copy(stage = stage) }
    }

    private fun save() = write(_state.value.times)

    /** Clears times saved on an earlier visit to this step too. */
    private fun turnOff() = write(emptyList())

    private fun write(times: List<ReminderTime>) {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            reminderCommands.setTimes(times)
                .onSuccess {
                    setStage(ReminderStage.Times)
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(ReminderStepEvent.Done)
                }
                .onFailure { error -> _state.update { it.copy(isSaving = false, error = error.toUiText()) } }
        }
    }

    private fun send(event: ReminderStepEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    companion object {
        /** Evening is when most people who work can study; a second time is the learner's choice. */
        val FIRST_SUGGESTION = ReminderTime(20, 0)

        /** What "Add a time" opens with: the way to work, the usual other half of the day. */
        val SECOND_SUGGESTION = ReminderTime(8, 0)

        private val DENIED_FOR_NOW = ReminderStage.Denied(permanently = false)

        private const val KEY_TIMES = "times"
        private const val KEY_STAGE = "stage"
        private const val MINUTES_PER_HOUR = 60

        private fun timeOfMinutes(minutes: Int) = ReminderTime(minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)

        private fun ReminderStage.key(): String = when (this) {
            ReminderStage.Times -> "times"
            ReminderStage.Rationale -> "rationale"
            is ReminderStage.Denied -> if (permanently) "blocked" else "denied"
        }

        private fun stageOf(key: String): ReminderStage? = when (key) {
            "times" -> ReminderStage.Times
            "rationale" -> ReminderStage.Rationale
            "denied" -> ReminderStage.Denied(permanently = false)
            "blocked" -> ReminderStage.Denied(permanently = true)
            else -> null
        }
    }
}
