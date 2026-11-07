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
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gyugle.gil.core.datastore.dataOrEmpty
import com.gyugle.gil.core.datastore.editCatching
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.lesson.impl.data.di.LessonPreferences
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import com.gyugle.gil.feature.lesson.impl.domain.repository.LessonRunRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import timber.log.Timber

/** Each run is JSON under a key named after its lesson. */
@Singleton
internal class DataStoreLessonRunRepository @Inject constructor(
    @param:LessonPreferences private val store: DataStore<Preferences>
) : LessonRunRepository {

    override suspend fun get(lessonId: LessonId): LessonRun? {
        val saved = store.dataOrEmpty().first()[key(lessonId)] ?: return null
        return try {
            json.decodeFromString<LessonRun>(saved).takeIf { it.isConsistent() }
        } catch (e: SerializationException) {
            // A broken entry only costs the place in one lesson; the lesson starts from the top.
            Timber.w("Dropped an unreadable lesson run: %s", e::class.simpleName)
            null
        } catch (e: IllegalArgumentException) {
            Timber.w("Dropped an unreadable lesson run: %s", e::class.simpleName)
            null
        }
    }

    override suspend fun save(lessonId: LessonId, run: LessonRun): EmptyOutcome<DataError.Local> =
        store.editCatching { it[key(lessonId)] = json.encodeToString(run) }

    override suspend fun clear(lessonId: LessonId): EmptyOutcome<DataError.Local> =
        store.editCatching { it.remove(key(lessonId)) }

    private fun key(lessonId: LessonId) = stringPreferencesKey("run.${lessonId.value}")

    /** A run edited by hand or written by a buggy build could point outside its own steps. */
    private fun LessonRun.isConsistent(): Boolean = stepCount >= 0 &&
        firstPassSize in 0..queue.size &&
        position in 0..queue.size &&
        queue.all { it in 0 until stepCount } &&
        outcomes.keys.all { it in 0 until stepCount } &&
        studiedMillis >= 0

    private companion object {
        val json = Json { ignoreUnknownKeys = true }
    }
}
