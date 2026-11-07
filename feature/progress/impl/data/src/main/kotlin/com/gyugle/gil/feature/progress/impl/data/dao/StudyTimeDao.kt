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
package com.gyugle.gil.feature.progress.impl.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gyugle.gil.feature.progress.impl.data.entity.StudyTimeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyTimeDao {
    // Two statements instead of one UPSERT: SQLite added ON CONFLICT DO UPDATE in 3.24, which
    // Android ships from API 30, and the app supports API 26.
    @Query("INSERT OR IGNORE INTO study_time (studyDay, durationMillis) VALUES (:day, 0)")
    suspend fun insertDayIfMissing(day: Long)

    @Query("UPDATE study_time SET durationMillis = durationMillis + :millis WHERE studyDay = :day")
    suspend fun addToDay(day: Long, millis: Long)

    @Query("SELECT * FROM study_time WHERE studyDay BETWEEN :from AND :to AND durationMillis > 0 ORDER BY studyDay")
    fun observeRange(from: Long, to: Long): Flow<List<StudyTimeEntity>>

    @Query("SELECT studyDay FROM study_time WHERE durationMillis >= :minMillis")
    fun observeDaysWithAtLeast(minMillis: Long): Flow<List<Long>>

    @Query("SELECT * FROM study_time ORDER BY studyDay")
    suspend fun getAll(): List<StudyTimeEntity>

    @Insert
    suspend fun insertAll(days: List<StudyTimeEntity>)

    @Query("DELETE FROM study_time")
    suspend fun deleteAll()
}
