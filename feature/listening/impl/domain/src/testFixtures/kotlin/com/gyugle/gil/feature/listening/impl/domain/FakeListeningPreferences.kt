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
package com.gyugle.gil.feature.listening.impl.domain

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Holds the values in memory. [error], when set, fails every write. */
class FakeListeningPreferences : ListeningPreferences {
    val pace = MutableStateFlow(ListeningPace())
    val lastPlayed = MutableStateFlow<LastPlayed?>(null)
    var error: DataError.Local? = null

    override fun observePace(): Flow<ListeningPace> = pace

    override suspend fun setPace(pace: ListeningPace): EmptyOutcome<DataError.Local> {
        error?.let { return Outcome.Failure(it) }
        this.pace.value = pace
        return Outcome.Success(Unit)
    }

    override fun observeLastPlayed(): Flow<LastPlayed?> = lastPlayed

    override suspend fun setLastPlayed(lastPlayed: LastPlayed?): EmptyOutcome<DataError.Local> {
        error?.let { return Outcome.Failure(it) }
        this.lastPlayed.value = lastPlayed
        return Outcome.Success(Unit)
    }
}
