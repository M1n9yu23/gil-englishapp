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
 * Reads the dictionary for other features. Like [CourseObserver], these are one-shot reads of
 * content that never changes while the app runs. Every call fails with [ContentError.UNAVAILABLE]
 * when reading fails.
 */
interface LexiconObserver {
    /** [ContentError.NOT_FOUND] when the dictionary has no entry [id]. */
    suspend fun getEntry(id: EntryId): Outcome<LexiconEntry, ContentError>

    /** The word a sense belongs to. [ContentError.NOT_FOUND] for an unknown sense. */
    suspend fun getWordOf(id: SenseId): Outcome<WordId, ContentError>

    /**
     * A line for each item in [ids], in the same order, to list them. Items the content no longer
     * has are left out.
     */
    suspend fun getItems(ids: List<StudyItemId>): Outcome<List<LessonItem>, ContentError>

    /**
     * Words and patterns matching [query]: English finds headwords and their forms by prefix,
     * Korean finds meanings that contain it. The headword itself comes first, then headwords that
     * start with the query, then the rest; within each, the earliest stage, then alphabetical.
     * A blank query finds nothing.
     */
    suspend fun search(query: String, limit: Int): Outcome<List<SearchHit>, ContentError>

    /** [ContentError.NOT_FOUND] when no sentence has [id]. */
    suspend fun getSentence(id: Long): Outcome<SentenceBreakdown, ContentError>
}
