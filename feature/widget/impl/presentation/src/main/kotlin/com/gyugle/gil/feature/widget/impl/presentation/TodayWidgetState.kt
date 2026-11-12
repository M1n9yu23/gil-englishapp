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
package com.gyugle.gil.feature.widget.impl.presentation

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.dictionary.api.SentenceLink
import com.gyugle.gil.feature.widget.impl.domain.model.TodayWidgetContent

/** What the widget draws. */
internal sealed interface TodayWidgetState {
    /** [sentence] is null until the learner has met something before today. */
    data class Ready(val dueCount: Int, val sentence: SentenceUi?) : TodayWidgetState

    /** The record or the dictionary could not be read; the widget only opens the app. */
    data object Unavailable : TodayWidgetState
}

/** The day's sentence, its Korean translation when the content gives one, and the link to its breakdown. */
internal data class SentenceUi(val text: String, val ko: String?, val link: String)

internal fun Outcome<TodayWidgetContent, ContentError>.toState(): TodayWidgetState = when (this) {
    is Outcome.Failure -> TodayWidgetState.Unavailable

    is Outcome.Success -> TodayWidgetState.Ready(
        dueCount = data.dueCount,
        sentence = data.sentence?.let {
            SentenceUi(
                text = it.sentence.text,
                ko = it.sentence.ko,
                link = SentenceLink(it.sentence.id, it.entryId.value).uri
            )
        }
    )
}
