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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.GoalCommands
import com.gyugle.gil.feature.goal.api.GoalObserver
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
 * The goal is saved only on "Next", so going back without it changes nothing. The choice so far
 * survives process death in [SavedStateHandle].
 */
@HiltViewModel
class GoalStepViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val goalObserver: GoalObserver,
    private val goalCommands: GoalCommands
) : ViewModel() {

    private val _state = MutableStateFlow(
        GoalStepState(
            selected = savedStateHandle.get<Int>(KEY_SELECTED)?.let(DailyGoal::ofMinutes) ?: DailyGoal.DEFAULT
        )
    )
    val state: StateFlow<GoalStepState> = _state.asStateFlow()

    private val eventChannel = Channel<GoalStepEvent>()
    val events: Flow<GoalStepEvent> = eventChannel.receiveAsFlow()

    init {
        // Coming back to this step shows the goal saved before, unless the learner picked another.
        if (KEY_SELECTED !in savedStateHandle) {
            viewModelScope.launch {
                val saved = goalObserver.observeGoal().first()
                if (KEY_SELECTED !in savedStateHandle) _state.update { it.copy(selected = saved) }
            }
        }
    }

    fun onAction(action: GoalStepAction) {
        when (action) {
            is GoalStepAction.OnGoalSelect -> {
                savedStateHandle[KEY_SELECTED] = action.goal.minutes
                _state.update { it.copy(selected = action.goal, error = null) }
            }

            GoalStepAction.OnNextClick -> save()
        }
    }

    private fun save() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            goalCommands.setGoal(_state.value.selected)
                .onSuccess {
                    _state.update { it.copy(isSaving = false) }
                    eventChannel.send(GoalStepEvent.Saved)
                }
                .onFailure { error -> _state.update { it.copy(isSaving = false, error = error.toUiText()) } }
        }
    }

    private companion object {
        const val KEY_SELECTED = "selected_minutes"
    }
}
