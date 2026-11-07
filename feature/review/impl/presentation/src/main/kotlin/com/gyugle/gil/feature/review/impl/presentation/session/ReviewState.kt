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
package com.gyugle.gil.feature.review.impl.presentation.session

import androidx.compose.runtime.Immutable

@Immutable
data class ReviewState(val phase: ReviewPhase = ReviewPhase.Loading)

@Immutable
sealed interface ReviewPhase {
    data object Loading : ReviewPhase

    /** The dictionary or the record could not be read. Trying again may work. */
    data object Failed : ReviewPhase

    /** Nothing is due. [nextInDays] is when the next card is, null when there are no cards. */
    data class Empty(val nextInDays: Int?) : ReviewPhase

    /**
     * Card [number] of [count]. [kind] names what to do, [hasVoice] whether play buttons show.
     * [feedback] is set once the card is answered.
     */
    data class Asking(
        val number: Int,
        val count: Int,
        val kind: CardKindUi,
        val card: CardUi,
        val hasVoice: Boolean,
        val feedback: FeedbackUi? = null
    ) : ReviewPhase {
        val canCheck: Boolean get() = feedback == null && (card as? CardUi.Typed)?.text?.isNotBlank() == true
    }

    data class Finished(val result: ResultUi) : ReviewPhase
}

/** What the learner is asked to do, which picks the instruction above the card. */
enum class CardKindUi {
    CHOOSE_MEANING,
    CHOOSE_ENGLISH,
    LISTEN_CHOOSE,
    CHOOSE_LETTER_BY_EAR,
    CHOOSE_LETTER_BY_NAME,
    FIND_SOUND,
    SPELL_BY_EAR,
    SPELL_BY_MEANING,
    FILL_BLANK
}
