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

import com.gyugle.gil.core.domain.ApplicationScope
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Called once from `Application.onCreate`. */
class ReminderStartup @Inject internal constructor(
    private val notifier: ReminderNotifier,
    private val reminders: Reminders,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    /**
     * Creates the channel, so the system settings list it from the first run, and sets the alarms
     * that are missing: a force stop clears them and sends no broadcast.
     */
    fun start() {
        notifier.createChannel()
        scope.launch { reminders.restoreAlarms() }
    }
}
