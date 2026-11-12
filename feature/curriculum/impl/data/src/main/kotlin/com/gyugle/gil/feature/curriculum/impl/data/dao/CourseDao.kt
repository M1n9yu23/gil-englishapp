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
import androidx.room.MapColumn
import androidx.room.Query
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageCanDoEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.UnitEntity

/**
 * Reads the course structure: stages, units and lessons. The content database is read-only.
 * Lists come in course order (`position`); callers rely on it and do not sort again.
 */
@Dao
interface CourseDao {
    @Query("SELECT * FROM stage ORDER BY position")
    suspend fun stages(): List<StageEntity>

    @Query("SELECT * FROM stage WHERE id = :id")
    suspend fun stage(id: String): StageEntity?

    @Query("SELECT * FROM stage_can_do ORDER BY stageId, position")
    suspend fun canDos(): List<StageCanDoEntity>

    @Query("SELECT * FROM unit ORDER BY stageId, position")
    suspend fun units(): List<UnitEntity>

    @Query("SELECT * FROM unit WHERE id = :id")
    suspend fun unit(id: String): UnitEntity?

    @Query("SELECT unitId, COUNT(*) AS lessonCount FROM lesson GROUP BY unitId")
    suspend fun lessonCounts(): Map<
        @MapColumn("unitId")
        String,
        @MapColumn("lessonCount")
        Int
        >

    /** How many items each stage's lessons introduce. A stage whose lessons introduce none is missing. */
    @Query(
        "SELECT unit.stageId AS stageId, COUNT(*) AS itemCount FROM lesson_item " +
            "JOIN lesson ON lesson.id = lesson_item.lessonId JOIN unit ON unit.id = lesson.unitId " +
            "GROUP BY unit.stageId"
    )
    suspend fun itemCounts(): Map<
        @MapColumn("stageId")
        String,
        @MapColumn("itemCount")
        Int
        >

    @Query("SELECT * FROM lesson WHERE unitId = :unitId ORDER BY position")
    suspend fun lessons(unitId: String): List<LessonEntity>

    @Query(
        "SELECT lesson.id AS lessonId, unit.stageId AS stageId FROM lesson " +
            "JOIN unit ON unit.id = lesson.unitId ORDER BY lesson.coursePosition"
    )
    suspend fun lessonStages(): List<LessonStageRow>

    @Query("SELECT id AS lessonId, unitId FROM lesson ORDER BY coursePosition")
    suspend fun lessonUnits(): List<LessonUnitRow>
}
