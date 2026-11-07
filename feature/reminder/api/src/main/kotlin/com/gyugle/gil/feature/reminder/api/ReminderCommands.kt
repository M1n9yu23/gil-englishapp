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
package com.gyugle.gil.feature.reminder.api

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome

interface ReminderCommands {
    /**
     * Saves [times] and schedules a reminder for each; an empty list turns reminders off. The same
     * time twice counts once.
     *
     * Saving does not ask for the notification permission. Without it the reminders are scheduled
     * but not shown, and they start to show once the learner allows notifications.
     *
     * @throws IllegalArgumentException for more than [ReminderTime.MAX_PER_DAY] different times.
     */
    suspend fun setTimes(times: List<ReminderTime>): EmptyOutcome<DataError.Local>
}
