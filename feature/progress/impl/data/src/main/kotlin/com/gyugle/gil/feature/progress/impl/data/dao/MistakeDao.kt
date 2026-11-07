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
import com.gyugle.gil.feature.progress.impl.data.entity.MistakeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {
    // Insert-then-update for the same reason as StudyTimeDao: no UPSERT before API 30.
    @Query("INSERT OR IGNORE INTO mistake (itemId, count, lastAt, lastDay) VALUES (:itemId, 0, :at, :day)")
    suspend fun insertIfMissing(itemId: String, at: Long, day: Long)

    @Query("UPDATE mistake SET count = count + 1, lastAt = :at, lastDay = :day WHERE itemId = :itemId")
    suspend fun addMistake(itemId: String, at: Long, day: Long)

    @Query("SELECT * FROM mistake ORDER BY lastAt DESC, itemId")
    fun observeAll(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistake ORDER BY lastAt, itemId")
    suspend fun getAll(): List<MistakeEntity>

    @Insert
    suspend fun insertAll(mistakes: List<MistakeEntity>)

    @Query("DELETE FROM mistake")
    suspend fun deleteAll()
}
