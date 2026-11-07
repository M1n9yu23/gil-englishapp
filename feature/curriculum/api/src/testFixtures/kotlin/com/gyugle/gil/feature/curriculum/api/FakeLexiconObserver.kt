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
package com.gyugle.gil.feature.curriculum.api

import com.gyugle.gil.core.domain.Outcome

/**
 * A [LexiconObserver] over [entries] and [sentences]. A missing ID is [ContentError.NOT_FOUND];
 * [error], when set, is what every call fails with. [search] returns [searchHits] for any query
 * that is not blank, and records each query in [searches].
 */
class FakeLexiconObserver(vararg entries: LexiconEntry) : LexiconObserver {
    val entries: MutableList<LexiconEntry> = entries.toMutableList()
    val sentences: MutableList<SentenceBreakdown> = mutableListOf()
    var searchHits: List<SearchHit> = emptyList()
    val searches: MutableList<String> = mutableListOf()
    var error: ContentError? = null

    override suspend fun getEntry(id: EntryId): Outcome<LexiconEntry, ContentError> = answer {
        entries.firstOrNull { it.id == id }
    }

    override suspend fun getWordOf(id: SenseId): Outcome<WordId, ContentError> = answer {
        entries.filterIsInstance<WordEntry>().firstOrNull { word -> word.senses.any { it.id == id } }?.id
    }

    override suspend fun getItems(ids: List<StudyItemId>): Outcome<List<LessonItem>, ContentError> {
        error?.let { return Outcome.Failure(it) }
        return Outcome.Success(ids.mapNotNull(::itemOf))
    }

    override suspend fun search(query: String, limit: Int): Outcome<List<SearchHit>, ContentError> {
        error?.let { return Outcome.Failure(it) }
        searches += query
        return Outcome.Success(if (query.isBlank()) emptyList() else searchHits.take(limit))
    }

    override suspend fun getSentence(id: Long): Outcome<SentenceBreakdown, ContentError> = answer {
        sentences.firstOrNull { it.sentence.id == id }
    }

    private fun itemOf(id: StudyItemId): LessonItem? = when (id) {
        is SenseId -> entries.filterIsInstance<WordEntry>().firstNotNullOfOrNull { word ->
            word.senses.firstOrNull { it.id == id }
                ?.let { LessonItem(id, word.text, it.ipa ?: word.ipa, it.meaningKo) }
        }

        is PatternId -> entries.filterIsInstance<PatternEntry>().firstOrNull { it.id == id }
            ?.let { LessonItem(id, it.text, null, it.meaningKo) }

        is SoundId -> entries.filterIsInstance<SoundEntry>().firstOrNull { it.id == id }
            ?.let { LessonItem(id, it.spelling, it.symbol, null) }
    }

    private inline fun <T> answer(find: () -> T?): Outcome<T, ContentError> {
        error?.let { return Outcome.Failure(it) }
        return find()?.let { Outcome.Success(it) } ?: Outcome.Failure(ContentError.NOT_FOUND)
    }
}
