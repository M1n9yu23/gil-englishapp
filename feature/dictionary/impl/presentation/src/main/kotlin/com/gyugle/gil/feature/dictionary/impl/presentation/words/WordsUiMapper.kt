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
package com.gyugle.gil.feature.dictionary.impl.presentation.words

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.dictionary.impl.domain.model.MistakeEntry
import com.gyugle.gil.feature.dictionary.impl.domain.model.RankedHit
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ItemRowUi
import com.gyugle.gil.feature.dictionary.impl.presentation.component.toLocalizedTextUi
import com.gyugle.gil.feature.dictionary.impl.presentation.toContentErrorUi

/** How many meanings a search row lists; the entry page has the rest. */
private const val MEANINGS_IN_A_ROW = 2

internal fun LessonItem.toItemRowUi(): ItemRowUi =
    ItemRowUi(id = id.value, text = text, ipa = ipa, meaningKo = meaningKo, isSound = id is SoundId)

internal fun MistakeEntry.toItemRowUi(): ItemRowUi = item.toItemRowUi().copy(mistakes = count)

/**
 * A search row lists the meanings that contain [query] first, so a Korean search shows why the
 * word matched. A word not learned yet names the earliest stage that teaches one of its meanings.
 */
internal fun RankedHit.toItemRowUi(query: String): ItemRowUi = ItemRowUi(
    id = hit.id.value,
    text = hit.text,
    ipa = hit.ipa,
    meaningKo = hit.meanings
        .sortedByDescending { query.isNotEmpty() && query in it.meaningKo }
        .take(MEANINGS_IN_A_ROW)
        .joinToString(", ") { it.meaningKo },
    stage = if (learned) null else hit.meanings.minByOrNull { it.stage.position }?.stage?.title?.toLocalizedTextUi()
)

internal fun List<RankedHit>.toSearchUi(query: String): SearchUi {
    if (isEmpty()) return SearchUi.NoResults(query)
    val (learned, notLearned) = partition { it.learned }
    return SearchUi.Results(
        learned = learned.map { it.toItemRowUi(query) },
        notLearned = notLearned.map { it.toItemRowUi(query) }
    )
}

internal fun <T> Outcome<List<T>, ContentError>.toWordListUi(row: (T) -> ItemRowUi): WordListUi = when (this) {
    is Outcome.Success -> WordListUi.Loaded(data.map(row))
    is Outcome.Failure -> WordListUi.Failed(error.toContentErrorUi())
}
