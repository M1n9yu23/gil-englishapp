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

import androidx.compose.runtime.Immutable
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.ThemeMode

/** @property values null until the goal, the reminder times and the settings have all been read. */
@Immutable
data class SettingsHomeState(
    val values: SettingsValues? = null,
    val dialog: SettingsDialog? = null,
    val isClearing: Boolean = false
)

@Immutable
data class SettingsValues(val goal: DailyGoal, val reminders: List<ReminderTime>, val settings: AppSettings)

sealed interface SettingsDialog {
    data object Goal : SettingsDialog

    data object Theme : SettingsDialog

    /** The first of the two questions before the record is erased. */
    data object Clear : SettingsDialog

    data object ClearAgain : SettingsDialog
}

sealed interface SettingsHomeAction {
    data object OnGoalClick : SettingsHomeAction

    data class OnGoalPick(val goal: DailyGoal) : SettingsHomeAction

    data object OnThemeClick : SettingsHomeAction

    data class OnThemePick(val theme: ThemeMode) : SettingsHomeAction

    data class OnHangulAidChange(val show: Boolean) : SettingsHomeAction

    data object OnClearClick : SettingsHomeAction

    data object OnClearNextClick : SettingsHomeAction

    data object OnClearConfirmClick : SettingsHomeAction

    data object OnDialogDismiss : SettingsHomeAction
}

sealed interface SettingsHomeEvent {
    data class ShowMessage(val message: UiText) : SettingsHomeEvent
}
