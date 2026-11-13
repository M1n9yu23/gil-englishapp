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
package com.gyugle.gil.feature.reminder.impl.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlin.time.Instant

/** A reminder time has come. */
internal class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val dueAt = intent.getLongExtra(ReminderAlarms.EXTRA_DUE_AT, 0L)
        runInBackground(context) { remind(dueAt = if (dueAt > 0) Instant.fromEpochMilliseconds(dueAt) else null) }
    }
}
