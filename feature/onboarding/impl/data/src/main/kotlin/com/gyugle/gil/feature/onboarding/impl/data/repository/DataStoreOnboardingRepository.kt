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
package com.gyugle.gil.feature.onboarding.impl.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.gyugle.gil.core.datastore.dataOrEmpty
import com.gyugle.gil.core.datastore.editCatching
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.onboarding.impl.data.di.OnboardingPreferences
import com.gyugle.gil.feature.onboarding.impl.domain.repository.OnboardingRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
internal class DataStoreOnboardingRepository @Inject constructor(
    @param:OnboardingPreferences private val store: DataStore<Preferences>
) : OnboardingRepository {

    override fun observeCompleted(): Flow<Boolean> = store.dataOrEmpty()
        .map { preferences -> preferences[COMPLETED] == true }
        .distinctUntilChanged()

    override suspend fun complete(): EmptyOutcome<DataError.Local> = store.editCatching { it[COMPLETED] = true }

    private companion object {
        val COMPLETED = booleanPreferencesKey("completed")
    }
}
