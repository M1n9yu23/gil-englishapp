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
package com.gyugle.gil.core.designsystem.component

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
import java.util.Calendar

/** A time of day as the phone shows times: "오후 8:00", "8:00 PM" or "20:00", following its 24-hour setting. */
@Composable
fun timeOfDayText(hour: Int, minute: Int): String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(hour, minute, locale) {
        val calendar = Calendar.getInstance(locale).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
        }
        DateFormat.getTimeFormat(context).format(calendar.time)
    }
}

/** The Material clock picker in a dialog, starting at [initialHour]:[initialMinute]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    title: String,
    confirmLabel: String,
    dismissLabel: String,
    onPick: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = { TimePicker(state = state) },
        confirmButton = { TextActionButton(text = confirmLabel, onClick = { onPick(state.hour, state.minute) }) },
        dismissButton = { TextActionButton(text = dismissLabel, onClick = onDismiss) }
    )
}
