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
package com.gyugle.gil.feature.curriculum.impl.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonStepEntity

/** Reads one lesson to play it. Lists come in the order the content gives. */
@Dao
interface LessonDao {
    @Query("SELECT * FROM lesson WHERE id = :id")
    suspend fun lesson(id: String): LessonEntity?

    @Query("SELECT * FROM lesson_step WHERE lessonId = :lessonId ORDER BY position")
    suspend fun steps(lessonId: String): List<LessonStepEntity>

    /** The items [lessonId] introduces, whichever table each lives in. */
    @Query(
        """
        SELECT li.itemId AS id, w.text AS text, COALESCE(s.ipa, w.ipa) AS ipa, s.meaningKo AS meaningKo,
            li.position AS position
        FROM lesson_item li JOIN sense s ON s.id = li.itemId JOIN word w ON w.id = s.wordId
        WHERE li.lessonId = :lessonId
        UNION ALL
        SELECT li.itemId, p.text, NULL, p.meaningKo, li.position
        FROM lesson_item li JOIN pattern p ON p.id = li.itemId
        WHERE li.lessonId = :lessonId
        UNION ALL
        SELECT li.itemId, so.spelling, so.symbol, NULL, li.position
        FROM lesson_item li JOIN sound so ON so.id = li.itemId
        WHERE li.lessonId = :lessonId
        ORDER BY position
        """
    )
    suspend fun items(lessonId: String): List<LessonItemRow>

    @Query("SELECT COUNT(*) FROM lesson_item WHERE lessonId = :lessonId")
    suspend fun itemCount(lessonId: String): Int

    @Query("SELECT id FROM lesson WHERE coursePosition > :coursePosition ORDER BY coursePosition LIMIT 1")
    suspend fun nextLessonId(coursePosition: Int): String?

    @Query("SELECT id FROM lesson ORDER BY coursePosition LIMIT 1")
    suspend fun firstLessonId(): String?
}
