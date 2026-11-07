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

import android.text.format.DateFormat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import com.gyugle.gil.feature.reminder.api.ReminderTime
import java.util.Calendar

/** The time as the phone shows times: "오후 8:00", "8:00 PM" or "20:00", following its 24-hour setting. */
@Composable
internal fun ReminderTime.displayText(): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(this, locale) {
        val calendar = Calendar.getInstance(locale).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        DateFormat.getTimeFormat(context).format(calendar.time)
    }
}

/** The Material clock picker in a dialog, starting at [initial]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReminderTimePickerDialog(
    initial: ReminderTime,
    onPick: (ReminderTime) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(stringResource(R.string.onboarding_reminder_picker_title)) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextActionButton(
                text = stringResource(R.string.onboarding_reminder_picker_confirm),
                onClick = { onPick(ReminderTime(state.hour, state.minute)) }
            )
        },
        dismissButton = {
            TextActionButton(text = stringResource(R.string.onboarding_reminder_picker_cancel), onClick = onDismiss)
        }
    )
}
