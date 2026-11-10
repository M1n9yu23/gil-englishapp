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
package com.gyugle.gil.feature.lesson.impl.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.gyugle.gil.core.datastore.dataOrEmpty
import com.gyugle.gil.core.datastore.editCatching
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.lesson.impl.data.di.LessonPreferences
import com.gyugle.gil.feature.lesson.impl.domain.repository.ScoringNoticeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
internal class DataStoreScoringNoticeRepository @Inject constructor(
    @param:LessonPreferences private val store: DataStore<Preferences>
) : ScoringNoticeRepository {

    override suspend fun wasShown(): Boolean = store.dataOrEmpty().first()[SHOWN] == true

    override suspend fun markShown(): EmptyOutcome<DataError.Local> = store.editCatching { it[SHOWN] = true }

    private companion object {
        val SHOWN = booleanPreferencesKey("scoring_notice_shown")
    }
}
