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

/** A study item (sense, pattern or sound) a lesson teaches for the first time. */
@Entity(
    tableName = "lesson_item",
    primaryKeys = ["lessonId", "position"],
    indices = [Index("itemId", unique = true)]
)
data class LessonItemEntity(val lessonId: String, val position: Int, val itemId: String)
