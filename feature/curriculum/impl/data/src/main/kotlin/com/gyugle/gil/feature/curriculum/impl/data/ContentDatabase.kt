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
package com.gyugle.gil.feature.curriculum.impl.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.gyugle.gil.feature.curriculum.impl.data.dao.CourseDao
import com.gyugle.gil.feature.curriculum.impl.data.dao.LessonDao
import com.gyugle.gil.feature.curriculum.impl.data.entity.GrammarEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.GrammarExampleEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonItemEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.LessonStepEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.PatternEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.PatternExampleEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SenseEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SenseExampleEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SentenceEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SoundEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SoundExampleEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageCanDoEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.UnitEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordCollocationEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordFamilyEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordFormEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordNoteEntity

/**
 * The read-only learning content, shipped as an asset built from content/ at compile time.
 *
 * The file is replaced whole when the content changes, never migrated, so the version only moves
 * when the schema does. tools/content-compiler fills the tables by column name, so renaming an
 * entity property needs the same change in the compiler.
 */
@Database(
    version = 1,
    entities = [
        StageEntity::class,
        StageCanDoEntity::class,
        UnitEntity::class,
        LessonEntity::class,
        LessonStepEntity::class,
        LessonItemEntity::class,
        WordEntity::class,
        WordFormEntity::class,
        WordNoteEntity::class,
        WordCollocationEntity::class,
        WordFamilyEntity::class,
        SenseEntity::class,
        SenseExampleEntity::class,
        SentenceEntity::class,
        PatternEntity::class,
        PatternExampleEntity::class,
        SoundEntity::class,
        SoundExampleEntity::class,
        GrammarEntity::class,
        GrammarExampleEntity::class
    ]
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao

    abstract fun lessonDao(): LessonDao
}
