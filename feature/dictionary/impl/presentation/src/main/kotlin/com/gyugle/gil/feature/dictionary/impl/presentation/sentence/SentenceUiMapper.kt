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
package com.gyugle.gil.feature.dictionary.impl.presentation.sentence

import com.gyugle.gil.feature.curriculum.api.ChunkRole
import com.gyugle.gil.feature.curriculum.api.SentenceBreakdown
import com.gyugle.gil.feature.dictionary.impl.presentation.R
import com.gyugle.gil.feature.dictionary.impl.presentation.component.label

/**
 * A gloss written with one part per chunk ("나는 / 마신다 / 커피를") goes under the chunks, so the
 * learner reads each English chunk with its Korean. Any other gloss stays whole.
 */
internal fun SentenceBreakdown.toSentenceUi(): SentenceUi {
    val text = sentence.text
    val glossParts = literalKo?.split(GLOSS_SEPARATOR)?.map { it.trim() }?.takeIf { it.size == chunks.size }
    return SentenceUi(
        text = text,
        ko = sentence.ko,
        words = words.map {
            SentenceWordUi(
                start = it.start,
                end = it.end,
                wordId = it.wordId.value,
                headword = it.headword,
                partOfSpeech = it.sense?.partOfSpeech?.label(),
                meaningKo = it.sense?.meaningKo
            )
        },
        chunks = chunks.mapIndexed { index, chunk ->
            ChunkUi(
                text = text.substring(chunk.start, chunk.end),
                role = chunk.role.label(),
                glossKo = glossParts?.get(index)
            )
        },
        literalKo = if (glossParts == null) literalKo else null,
        grammar = grammar?.let { GrammarUi(title = it.titleKo, paragraphs = it.paragraphs) }
    )
}

private fun ChunkRole.label(): Int = when (this) {
    ChunkRole.WHO -> R.string.dictionary_role_who
    ChunkRole.DO -> R.string.dictionary_role_do
    ChunkRole.WHAT -> R.string.dictionary_role_what
    ChunkRole.WHERE -> R.string.dictionary_role_where
    ChunkRole.WHEN -> R.string.dictionary_role_when
}

private const val GLOSS_SEPARATOR = "/"
