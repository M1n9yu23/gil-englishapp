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

/** The permission lives with the activity, so the Root answers these; the ViewModel cannot. */
sealed interface ReminderStepEvent {
    data object CheckPermission : ReminderStepEvent

    data object RequestPermission : ReminderStepEvent

    data object OpenAppSettings : ReminderStepEvent

    /** The times are saved, or reminders are off; go on to the sound check. */
    data object Done : ReminderStepEvent
}
