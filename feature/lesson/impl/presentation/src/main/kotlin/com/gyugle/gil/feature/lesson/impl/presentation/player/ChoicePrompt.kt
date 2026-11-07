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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import androidx.compose.runtime.Immutable

/** What sits above the options of a [StepUi.Choice]. */
@Immutable
sealed interface ChoicePrompt {
    /** An English sentence to understand. */
    data class English(val text: String) : ChoicePrompt

    /** A sentence only heard. [text] is shown once answered, or when there is no voice. */
    data class Audio(val text: String?) : ChoicePrompt

    /** A Korean prompt answered in English. Backticks mark English inside it. */
    data class Korean(val text: String) : ChoicePrompt

    /** Two words told apart by sound. */
    data object SoundPair : ChoicePrompt

    /** An English sentence with `___` for the blank, and its meaning in Korean. */
    data class Blank(val text: String, val ko: String) : ChoicePrompt

    data class Dialogue(val lines: List<DialogueLineUi>, val question: String?) : ChoicePrompt

    /**
     * A passage and one of its questions. The translations stay hidden until every question is
     * answered, when [showsTranslations] turns true.
     */
    data class Passage(
        val sentences: List<SentenceUi>,
        val showsTranslations: Boolean,
        val question: String,
        val questionNumber: Int,
        val questionCount: Int
    ) : ChoicePrompt
}
