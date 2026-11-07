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

/** A card as the screen draws it. */
@Immutable
sealed interface CardUi {
    /**
     * Options to pick from. [picked] is the learner's pick and [correct] the right option, both
     * null until the card is answered.
     */
    data class Choice(
        val prompt: ChoicePromptUi,
        val options: List<AnswerUi>,
        val picked: Int? = null,
        val correct: Int? = null
    ) : CardUi

    /** An answer to type into [text]. */
    data class Typed(val prompt: TypedPromptUi, val text: String = "") : CardUi
}
