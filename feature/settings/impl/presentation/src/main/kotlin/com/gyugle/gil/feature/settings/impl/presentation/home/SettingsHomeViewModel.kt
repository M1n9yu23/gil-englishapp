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
package com.gyugle.gil.feature.settings.impl.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.feature.goal.api.GoalCommands
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.progress.api.ProgressBackup
import com.gyugle.gil.feature.reminder.api.ReminderObserver
import com.gyugle.gil.feature.settings.impl.domain.model.AppLanguage
import com.gyugle.gil.feature.settings.impl.domain.repository.AppLanguageRepository
import com.gyugle.gil.feature.settings.impl.domain.repository.SettingsRepository
import com.gyugle.gil.feature.settings.impl.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The list of settings. Every change is saved at once, so there is nothing to confirm on leaving.
 * An open dialog survives rotation with the ViewModel; after process death the list opens without it.
 */
@HiltViewModel
class SettingsHomeViewModel @Inject constructor(
    goalObserver: GoalObserver,
    reminderObserver: ReminderObserver,
    private val goalCommands: GoalCommands,
    private val settingsRepository: SettingsRepository,
    private val appLanguage: AppLanguageRepository,
    private val progressBackup: ProgressBackup
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsHomeState(language = readLanguage()))
    val state: StateFlow<SettingsHomeState> = _state.asStateFlow()

    private val eventChannel = Channel<SettingsHomeEvent>()
    val events: Flow<SettingsHomeEvent> = eventChannel.receiveAsFlow()

    init {
        combine(
            goalObserver.observeGoal(),
            reminderObserver.observeTimes(),
            settingsRepository.observeSettings(),
            ::SettingsValues
        )
            .onEach { values -> _state.update { it.copy(values = values) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: SettingsHomeAction) {
        when (action) {
            SettingsHomeAction.OnGoalClick -> showDialog(SettingsDialog.Goal)
            is SettingsHomeAction.OnGoalPick -> closeAndSave { goalCommands.setGoal(action.goal) }
            SettingsHomeAction.OnThemeClick -> showDialog(SettingsDialog.Theme)
            is SettingsHomeAction.OnThemePick -> closeAndSave { settingsRepository.setTheme(action.theme) }
            SettingsHomeAction.OnLanguageClick -> showDialog(SettingsDialog.Language)
            is SettingsHomeAction.OnLanguagePick -> setLanguage(action.language)
            is SettingsHomeAction.OnHangulAidChange -> closeAndSave { settingsRepository.setShowHangulAid(action.show) }
            SettingsHomeAction.OnClearClick -> showDialog(SettingsDialog.Clear)
            SettingsHomeAction.OnClearNextClick -> showDialog(SettingsDialog.ClearAgain)
            SettingsHomeAction.OnClearConfirmClick -> clearRecord()
            SettingsHomeAction.OnDialogDismiss -> showDialog(null)
            SettingsHomeAction.OnResumed -> _state.update { it.copy(language = readLanguage()) }
        }
    }

    private fun readLanguage(): AppLanguage? = if (appLanguage.canChoose) appLanguage.current() else null

    /** The screen is recreated in the new language; the dialog is already closed by then. */
    private fun setLanguage(language: AppLanguage) {
        _state.update { it.copy(dialog = null, language = language) }
        appLanguage.set(language)
    }

    private fun showDialog(dialog: SettingsDialog?) = _state.update { it.copy(dialog = dialog) }

    private fun closeAndSave(save: suspend () -> EmptyOutcome<DataError.Local>) {
        showDialog(null)
        viewModelScope.launch { save().onFailure { error -> send(error.toUiText()) } }
    }

    private fun clearRecord() {
        if (_state.value.isClearing) return
        _state.update { it.copy(dialog = null, isClearing = true) }
        viewModelScope.launch {
            progressBackup.clear()
                .onSuccess { send(UiText.StringResource(R.string.settings_cleared)) }
                .onFailure { error -> send(error.toUiText()) }
            _state.update { it.copy(isClearing = false) }
        }
    }

    private suspend fun send(message: UiText) = eventChannel.send(SettingsHomeEvent.ShowMessage(message))
}
