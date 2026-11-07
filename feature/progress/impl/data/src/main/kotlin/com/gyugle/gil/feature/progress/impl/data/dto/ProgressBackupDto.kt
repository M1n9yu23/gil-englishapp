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
package com.gyugle.gil.feature.progress.impl.data.dto

import kotlinx.serialization.Serializable

/**
 * The learning record in a backup file, format version 1.
 *
 * Field names are part of the file format: renaming one breaks files people already saved. A
 * change that older files cannot satisfy needs a new [version] and a reader for the old one.
 * Days are written as dates (`2026-11-07`), moments as ISO-8601 in UTC.
 */
@Serializable
data class ProgressBackupDto(
    val version: Int,
    val cards: List<CardDto>,
    val reviews: List<ReviewDto>,
    val lessons: List<LessonCompletionDto>,
    val studyTime: List<StudyTimeDto>,
    val saved: List<SavedItemDto>,
    val mistakes: List<MistakeDto>
) {
    companion object {
        const val VERSION = 1
    }
}
