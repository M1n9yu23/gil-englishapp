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
package com.gyugle.gil.feature.progress.impl.data.repository

import androidx.room.withTransaction
import com.gyugle.gil.core.database.databaseCall
import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.domain.flatMap
import com.gyugle.gil.core.domain.map
import com.gyugle.gil.core.domain.mapError
import com.gyugle.gil.feature.progress.api.ProgressBackup
import com.gyugle.gil.feature.progress.api.ProgressBackupPreview
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.impl.data.ProgressDatabase
import com.gyugle.gil.feature.progress.impl.data.ProgressTables
import com.gyugle.gil.feature.progress.impl.data.dto.ProgressBackupDto
import com.gyugle.gil.feature.progress.impl.data.mapper.toBackupDto
import com.gyugle.gil.feature.progress.impl.data.mapper.toTablesOrNull
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject

/** @param jsonDispatcher runs the JSON encoding and parsing, which can take a while for a long record. */
class RoomProgressBackup internal constructor(
    private val database: ProgressDatabase,
    private val jsonDispatcher: CoroutineDispatcher
) : ProgressBackup {
    @Inject
    constructor(database: ProgressDatabase) : this(database, Dispatchers.Default)

    override suspend fun export(): Outcome<String, DataError.Local> =
        databaseCall { database.withTransaction { readTables() } }
            .map { tables -> withContext(jsonDispatcher) { json.encodeToString(tables.toBackupDto()) } }

    override suspend fun preview(section: String): Outcome<ProgressBackupPreview, BackupError> =
        withContext(jsonDispatcher) { parse(section) }.map { tables ->
            val days = tables.reviews.map { it.studyDay } +
                tables.lessons.map { it.studyDay } +
                tables.studyTime.filter { it.durationMillis > 0 }.map { it.studyDay }
            ProgressBackupPreview(
                itemCount = tables.cards.size,
                completedLessonCount = tables.lessons.map { it.lessonId }.toSet().size,
                lastStudyDay = days.maxOrNull()?.let(::StudyDay)
            )
        }

    override suspend fun restore(section: String): EmptyOutcome<BackupError> =
        withContext(jsonDispatcher) { parse(section) }.flatMap { tables ->
            databaseCall { database.withTransaction { replaceTables(tables) } }.mapError(BackupError::Storage)
        }

    override suspend fun clear(): EmptyOutcome<DataError.Local> =
        databaseCall { database.withTransaction { replaceTables(EMPTY) } }

    /** Reads the version first so a newer file is reported as newer rather than as broken. */
    private fun parse(section: String): Outcome<ProgressTables, BackupError> {
        // SerializationException and the date and time parse errors are IllegalArgumentExceptions.
        val root = try {
            json.parseToJsonElement(section).jsonObject
        } catch (_: IllegalArgumentException) {
            return Outcome.Failure(BackupError.Format.MALFORMED)
        }
        val version = (root["version"] as? JsonPrimitive)?.intOrNull
        when {
            version == null || version < 1 -> return Outcome.Failure(BackupError.Format.MALFORMED)
            version > ProgressBackupDto.VERSION -> return Outcome.Failure(BackupError.Format.NEWER_VERSION)
        }
        val dto = try {
            json.decodeFromJsonElement(ProgressBackupDto.serializer(), root)
        } catch (_: IllegalArgumentException) {
            return Outcome.Failure(BackupError.Format.MALFORMED)
        }
        return dto.toTablesOrNull()?.let { Outcome.Success(it) } ?: Outcome.Failure(BackupError.Format.MALFORMED)
    }

    private suspend fun readTables() = ProgressTables(
        cards = database.cardDao().getAll(),
        reviews = database.reviewLogDao().getAll(),
        lessons = database.lessonCompletionDao().getAll(),
        studyTime = database.studyTimeDao().getAll(),
        saved = database.savedItemDao().getAll(),
        mistakes = database.mistakeDao().getAll()
    )

    private suspend fun replaceTables(tables: ProgressTables) {
        database.cardDao().run {
            deleteAll()
            insertAll(tables.cards)
        }
        database.reviewLogDao().run {
            deleteAll()
            insertAll(tables.reviews)
        }
        database.lessonCompletionDao().run {
            deleteAll()
            insertAll(tables.lessons)
        }
        database.studyTimeDao().run {
            deleteAll()
            insertAll(tables.studyTime)
        }
        database.savedItemDao().run {
            deleteAll()
            insertAll(tables.saved)
        }
        database.mistakeDao().run {
            deleteAll()
            insertAll(tables.mistakes)
        }
    }

    private companion object {
        /** Strict: an unknown field means the file is not what this version wrote. */
        val json = Json

        val EMPTY = ProgressTables(
            cards = emptyList(),
            reviews = emptyList(),
            lessons = emptyList(),
            studyTime = emptyList(),
            saved = emptyList(),
            mistakes = emptyList()
        )
    }
}
