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
package com.gyugle.gil.feature.settings.impl.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.gyugle.gil.core.designsystem.component.timeOfDayText
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.api.ThemeMode
import com.gyugle.gil.feature.settings.impl.domain.model.AppLanguage
import com.gyugle.gil.feature.settings.impl.presentation.R

@Composable
internal fun DailyGoal.label(): String = pluralStringResource(R.plurals.settings_goal_minutes, minutes, minutes)

@Composable
internal fun ThemeMode.label(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.LIGHT -> R.string.settings_theme_light
        ThemeMode.DARK -> R.string.settings_theme_dark
    }
)

/** A language is named in itself, so it can be found whichever language the screen is in. */
@Composable
internal fun AppLanguage.label(): String = stringResource(
    when (this) {
        AppLanguage.SYSTEM -> R.string.settings_language_system
        AppLanguage.KOREAN -> R.string.settings_language_korean
        AppLanguage.ENGLISH -> R.string.settings_language_english
    }
)

@Composable
internal fun Accent.label(): String = stringResource(
    when (this) {
        Accent.AMERICAN -> R.string.settings_accent_american
        Accent.BRITISH -> R.string.settings_accent_british
    }
)

@Composable
internal fun SpeechSpeed.label(): String = stringResource(
    when (this) {
        SpeechSpeed.NORMAL -> R.string.settings_speed_normal
        SpeechSpeed.SLOW -> R.string.settings_speed_slow
    }
)

@Composable
internal fun ReminderTime.label(): String = timeOfDayText(hour, minute)

/** "Off", one time, or both times in the order of the day. */
@Composable
internal fun List<ReminderTime>.summary(): String = when (size) {
    0 -> stringResource(R.string.settings_reminders_off)
    1 -> first().label()
    else -> stringResource(R.string.settings_reminders_two, this[0].label(), this[1].label())
}
