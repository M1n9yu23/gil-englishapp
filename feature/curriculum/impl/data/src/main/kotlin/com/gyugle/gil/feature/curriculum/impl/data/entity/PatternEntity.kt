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

/** A chunk such as "I'd like ~", reviewed like a sense. */
@Entity(tableName = "pattern", indices = [Index("lessonId")])
data class PatternEntity(
    @PrimaryKey val id: String,
    val text: String,
    val meaningKo: String,
    val noteKo: String?,
    val stageId: String,
    val lessonId: String?
)
