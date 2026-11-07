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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.flatMap
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.GoalCommands
import com.gyugle.gil.feature.onboarding.impl.domain.repository.OnboardingRepository
import com.gyugle.gil.feature.reminder.api.ReminderCommands
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Skipping ends the first run; the app then leaves it on its own, so there is no event. It sets the
 * defaults first, because a learner may have saved a goal or reminders and come back to this screen.
 */
@HiltViewModel
class WelcomeViewModel @Inject constructor(
    private val onboardingRepository: OnboardingRepository,
    private val goalCommands: GoalCommands,
    private val reminderCommands: ReminderCommands
) : ViewModel() {

    private val _state = MutableStateFlow(WelcomeState())
    val state: StateFlow<WelcomeState> = _state.asStateFlow()

    fun onAction(action: WelcomeAction) {
        when (action) {
            WelcomeAction.OnSkipClick -> skip()
        }
    }

    private fun skip() {
        if (_state.value.isFinishing) return
        viewModelScope.launch {
            _state.update { it.copy(isFinishing = true, error = null) }
            goalCommands.setGoal(DailyGoal.DEFAULT)
                .flatMap { reminderCommands.setTimes(emptyList()) }
                .flatMap { onboardingRepository.complete() }
                .onFailure { error -> _state.update { it.copy(isFinishing = false, error = error.toUiText()) } }
        }
    }
}
