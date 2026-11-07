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
package com.gyugle.gil.feature.goal.impl.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.gyugle.gil.core.datastore.dataOrEmpty
import com.gyugle.gil.core.datastore.editCatching
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.GoalCommands
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.goal.impl.data.di.GoalPreferences
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Keeps the goal as its length in minutes, so renaming a constant never changes a saved goal. */
@Singleton
internal class DataStoreGoalRepository @Inject constructor(
    @param:GoalPreferences private val store: DataStore<Preferences>
) : GoalObserver,
    GoalCommands {

    override fun observeGoal(): Flow<DailyGoal> = store.dataOrEmpty()
        .map { preferences -> preferences[MINUTES]?.let(DailyGoal::ofMinutes) ?: DailyGoal.DEFAULT }
        .distinctUntilChanged()

    override suspend fun setGoal(goal: DailyGoal): EmptyOutcome<DataError.Local> =
        store.editCatching { it[MINUTES] = goal.minutes }

    private companion object {
        val MINUTES = intPreferencesKey("daily_goal_minutes")
    }
}
