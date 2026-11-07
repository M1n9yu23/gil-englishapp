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

/** An option. [ipa] shows under the text once the card is answered, for words picked by sound. */
@Immutable
data class AnswerUi(val text: String, val english: Boolean, val ipa: String? = null)

@Immutable
sealed interface ChoicePromptUi {
    /** English to understand, heard by the play button when [speakable]. */
    data class Meaning(val english: String, val ipa: String?, val speakable: Boolean) : ChoicePromptUi

    /** A Korean meaning; backticks mark English inside it. */
    data class English(val meaningKo: String) : ChoicePromptUi

    /** English only heard. [revealed] shows it once the card is answered. */
    data class Listen(val revealed: String?) : ChoicePromptUi

    /** A letter's name, read as [nameIpa] when there is no voice or once the card is answered. */
    data class Letter(val nameIpa: String, val showsIpa: Boolean) : ChoicePromptUi

    /** A sound's symbol, as IPA. */
    data class Sound(val symbol: String) : ChoicePromptUi
}

@Immutable
sealed interface TypedPromptUi {
    /** A word to spell from its meaning and IPA, also heard when [speakable]. */
    data class Spell(val meaningKo: String, val ipa: String?, val speakable: Boolean) : TypedPromptUi

    /** A sentence with `___` for the missing word, and its translation. */
    data class Blank(val text: String, val translation: String?) : TypedPromptUi
}
