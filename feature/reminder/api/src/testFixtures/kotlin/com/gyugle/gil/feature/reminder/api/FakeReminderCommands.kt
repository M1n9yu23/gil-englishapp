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
import com.gyugle.gil.core.domain.Outcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Records every save, so a test can tell "turned off" from "never saved". */
class FakeReminderCommands :
    ReminderObserver,
    ReminderCommands {
    val times = MutableStateFlow<List<ReminderTime>>(emptyList())
    val saved = mutableListOf<List<ReminderTime>>()

    /** Returned by [setTimes] instead of success. */
    var error: DataError.Local? = null

    /** When set, [setTimes] suspends until the test completes it, to observe the saving state. */
    var saving: CompletableDeferred<Unit>? = null

    override fun observeTimes(): Flow<List<ReminderTime>> = times

    override suspend fun setTimes(times: List<ReminderTime>): EmptyOutcome<DataError.Local> {
        saved += times
        saving?.await()
        error?.let { return Outcome.Failure(it) }
        this.times.value = times
        return Outcome.Success(Unit)
    }
}
