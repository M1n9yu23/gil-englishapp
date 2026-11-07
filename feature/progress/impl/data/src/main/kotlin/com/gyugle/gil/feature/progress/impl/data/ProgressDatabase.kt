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
package com.gyugle.gil.feature.progress.impl.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.gyugle.gil.feature.progress.impl.data.dao.CardDao
import com.gyugle.gil.feature.progress.impl.data.dao.LessonCompletionDao
import com.gyugle.gil.feature.progress.impl.data.dao.MistakeDao
import com.gyugle.gil.feature.progress.impl.data.dao.ReviewLogDao
import com.gyugle.gil.feature.progress.impl.data.dao.SavedItemDao
import com.gyugle.gil.feature.progress.impl.data.dao.StudyTimeDao
import com.gyugle.gil.feature.progress.impl.data.entity.CardEntity
import com.gyugle.gil.feature.progress.impl.data.entity.LessonCompletionEntity
import com.gyugle.gil.feature.progress.impl.data.entity.MistakeEntity
import com.gyugle.gil.feature.progress.impl.data.entity.ReviewLogEntity
import com.gyugle.gil.feature.progress.impl.data.entity.SavedItemEntity
import com.gyugle.gil.feature.progress.impl.data.entity.StudyTimeEntity

/**
 * The learner's record. Unlike the content database it is never rebuilt: every schema change
 * needs a migration that keeps the data, with a test in ProgressMigrationTest.
 */
@Database(
    version = ProgressDatabase.VERSION,
    entities = [
        CardEntity::class,
        ReviewLogEntity::class,
        LessonCompletionEntity::class,
        StudyTimeEntity::class,
        SavedItemEntity::class,
        MistakeEntity::class
    ]
)
abstract class ProgressDatabase : RoomDatabase() {
    abstract fun cardDao(): CardDao

    abstract fun reviewLogDao(): ReviewLogDao

    abstract fun lessonCompletionDao(): LessonCompletionDao

    abstract fun studyTimeDao(): StudyTimeDao

    abstract fun savedItemDao(): SavedItemDao

    abstract fun mistakeDao(): MistakeDao

    companion object {
        const val VERSION = 1

        /** Named in the app's backup rules (backup_rules.xml, data_extraction_rules.xml). */
        const val FILE_NAME = "progress.db"

        /** Every migration, oldest first. Add each new one here. */
        val MIGRATIONS: Array<Migration> = emptyArray()
    }
}
