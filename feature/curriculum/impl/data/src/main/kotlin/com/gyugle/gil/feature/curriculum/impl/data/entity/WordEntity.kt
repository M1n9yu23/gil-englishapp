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
import androidx.room.PrimaryKey

@Entity(tableName = "word")
data class WordEntity(
    @PrimaryKey val id: String,
    val text: String,
    /** American IPA without slashes. */
    val ipa: String,
    /** Hangul pronunciation aid, only for words of the first stages. */
    val hangul: String?,
    /**
     * Lowercase headword and all its forms, each preceded by a space (" run runs ran running"),
     * so `LIKE '% ru%'` finds any form by prefix.
     */
    val searchKey: String
)
