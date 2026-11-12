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
package com.gyugle.gil.feature.widget.impl.domain

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.EntryId
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.widget.impl.domain.model.DaySentence
import com.gyugle.gil.feature.widget.impl.domain.model.TodayWidgetContent
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Items tried, in the day's order, before the widget goes without a sentence. */
internal const val MAX_SENTENCE_LOOKUPS = 10

/**
 * Reads what the widget shows on one day. A failure to read the learner's record is thrown, as
 * [ProgressObserver]'s flows throw it.
 */
class TodayWidgetReader @Inject constructor(
    private val progress: ProgressObserver,
    private val lexicon: LexiconObserver
) {
    /**
     * The widget's content on [day], again whenever the record changes. Fails with
     * [ContentError.UNAVAILABLE] when the dictionary cannot be read.
     */
    fun observe(day: StudyDay): Flow<Outcome<TodayWidgetContent, ContentError>> {
        val sentenceItems = progress.observeCards().map { sentenceItems(day, it).toSet() }.distinctUntilChanged()
        // The sentence is looked up only when the items change, not for every card reviewed.
        val sentence = sentenceItems.map { daySentence(day, it.toList()) }
        return combine(progress.observeDueCards(day), sentence) { due, read ->
            when (read) {
                is Outcome.Failure -> read
                is Outcome.Success -> Outcome.Success(TodayWidgetContent(due.size, read.data))
            }
        }
    }

    private suspend fun daySentence(day: StudyDay, items: List<StudyItemId>): Outcome<DaySentence?, ContentError> {
        for (item in dayOrder(day, items).take(MAX_SENTENCE_LOOKUPS)) {
            when (val read = examplesOf(item)) {
                is Outcome.Failure -> if (read.error != ContentError.NOT_FOUND) return read

                is Outcome.Success -> {
                    val (entryId, examples) = read.data
                    if (examples.isNotEmpty()) {
                        return Outcome.Success(DaySentence(examples[exampleIndex(day, examples.size)], entryId))
                    }
                }
            }
        }
        return Outcome.Success(null)
    }

    /** The page the item is on, and its examples. A sense is on its word's page. */
    private suspend fun examplesOf(item: StudyItemId): Outcome<Pair<EntryId, List<Sentence>>, ContentError> =
        when (item) {
            is SenseId -> when (val word = lexicon.getWordOf(item)) {
                is Outcome.Failure -> word

                is Outcome.Success -> when (val entry = lexicon.getEntry(word.data)) {
                    is Outcome.Failure -> entry

                    is Outcome.Success -> (entry.data as? WordEntry)?.senses?.firstOrNull { it.id == item }
                        ?.let { Outcome.Success(word.data to it.examples) }
                        ?: Outcome.Failure(ContentError.NOT_FOUND)
                }
            }

            is PatternId -> when (val entry = lexicon.getEntry(item)) {
                is Outcome.Failure -> entry

                is Outcome.Success -> (entry.data as? PatternEntry)
                    ?.let { Outcome.Success(item to it.examples) }
                    ?: Outcome.Failure(ContentError.NOT_FOUND)
            }

            is SoundId -> Outcome.Failure(ContentError.NOT_FOUND)
        }
}
