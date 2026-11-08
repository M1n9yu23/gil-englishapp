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
package com.gyugle.gil.feature.goal.api

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeGoalRepository(goal: DailyGoal = DailyGoal.DEFAULT) :
    GoalObserver,
    GoalCommands {
    val goal = MutableStateFlow(goal)

    /** Returned by [setGoal] instead of success, leaving [goal] unchanged. */
    var error: DataError.Local? = null

    override fun observeGoal(): Flow<DailyGoal> = goal

    override suspend fun setGoal(goal: DailyGoal): EmptyOutcome<DataError.Local> {
        error?.let { return Outcome.Failure(it) }
        this.goal.value = goal
        return Outcome.Success(Unit)
    }
}
