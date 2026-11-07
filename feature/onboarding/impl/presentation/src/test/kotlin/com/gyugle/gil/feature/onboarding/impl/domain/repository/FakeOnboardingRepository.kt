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
package com.gyugle.gil.feature.onboarding.impl.domain.repository

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeOnboardingRepository : OnboardingRepository {
    val completed = MutableStateFlow(false)

    /** Returned by [complete] instead of success, leaving [completed] unchanged. */
    var error: DataError.Local? = null

    var completeCount = 0
        private set

    override fun observeCompleted(): Flow<Boolean> = completed

    override suspend fun complete(): EmptyOutcome<DataError.Local> {
        completeCount++
        error?.let { return Outcome.Failure(it) }
        completed.value = true
        return Outcome.Success(Unit)
    }
}
