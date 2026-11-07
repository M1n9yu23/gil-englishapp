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
import com.gyugle.gil.feature.reminder.impl.data.di.ReminderReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

/**
 * Keeps the process alive until [work] ends. The system allows a receiver about ten seconds, far
 * more than reading the preferences and the record takes. [Reminders] catches its own failures,
 * so [work] always returns and the broadcast is always finished.
 */
internal fun BroadcastReceiver.runInBackground(context: Context, work: suspend Reminders.() -> Unit) {
    val entryPoint = EntryPointAccessors.fromApplication(context, ReminderReceiverEntryPoint::class.java)
    val pending = goAsync()
    entryPoint.applicationScope().launch {
        try {
            entryPoint.reminders().work()
        } finally {
            pending.finish()
        }
    }
}
