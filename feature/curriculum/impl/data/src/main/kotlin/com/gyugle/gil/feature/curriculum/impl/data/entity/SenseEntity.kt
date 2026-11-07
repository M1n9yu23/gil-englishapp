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
package com.gyugle.gil.feature.curriculum.impl.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One meaning of a word, the unit of review. */
@Entity(tableName = "sense", indices = [Index("wordId"), Index("lessonId")])
data class SenseEntity(
    @PrimaryKey val id: String,
    val wordId: String,
    val position: Int,
    /** Part of speech, such as `noun` or `verb`. */
    val pos: String,
    val meaningKo: String,
    val definitionEn: String?,
    /** IPA when this sense is said differently from the word (read, lead). */
    val ipa: String?,
    /** The stage the sense belongs to: where it is introduced, or its level if no lesson has it yet. */
    val stageId: String,
    /** The lesson that introduces it, or null for a sense the course does not teach yet. */
    val lessonId: String?
)
