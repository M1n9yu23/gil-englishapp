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

import androidx.compose.runtime.Immutable
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.reminder.api.ReminderTime

@Immutable
data class ReminderStepState(
    /** In the order of the day. */
    val times: List<ReminderTime> = listOf(ReminderStepViewModel.FIRST_SUGGESTION),
    val stage: ReminderStage = ReminderStage.Times,
    /** The time whose picker is open: an index into [times], or [NEW_TIME] for an added one. */
    val editing: Int? = null,
    val isSaving: Boolean = false,
    val error: UiText? = null
) {
    val canAddTime: Boolean get() = times.size < ReminderTime.MAX_PER_DAY

    companion object {
        const val NEW_TIME = -1
    }
}
