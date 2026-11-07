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

/**
 * An English sentence used anywhere in the content.
 *
 * [tokens] is a JSON array of `{start, end, word?, sense?}` linking each word (UTF-16 offsets into
 * [text]) to the lexicon; [chunks] is a JSON array of `{start, end, role}` for the sentence
 * breakdown, present only on sentences that have one.
 */
@Entity(tableName = "sentence")
data class SentenceEntity(
    @PrimaryKey val id: Long,
    val text: String,
    val translationKo: String?,
    val tokens: String,
    val chunks: String?,
    val literalKo: String?,
    val grammarId: String?
)
