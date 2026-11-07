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
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gyugle.gil.feature.progress.impl.data.entity.SavedItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedItemDao {
    /** Keeps the first save time when the item is already on the list. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(item: SavedItemEntity)

    @Query("DELETE FROM saved_item WHERE itemId = :itemId")
    suspend fun delete(itemId: String)

    @Query("SELECT itemId FROM saved_item ORDER BY savedAt DESC, itemId")
    fun observeItemIds(): Flow<List<String>>

    @Query("SELECT * FROM saved_item ORDER BY savedAt, itemId")
    suspend fun getAll(): List<SavedItemEntity>

    @Insert
    suspend fun insertAll(items: List<SavedItemEntity>)

    @Query("DELETE FROM saved_item")
    suspend fun deleteAll()
}
