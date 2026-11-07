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
import com.gyugle.gil.feature.progress.impl.data.entity.ReviewLogEntity

@Dao
interface ReviewLogDao {
    @Insert
    suspend fun insert(log: ReviewLogEntity)

    @Query("SELECT * FROM review_log ORDER BY id")
    suspend fun getAll(): List<ReviewLogEntity>

    @Insert
    suspend fun insertAll(logs: List<ReviewLogEntity>)

    @Query("DELETE FROM review_log")
    suspend fun deleteAll()
}
