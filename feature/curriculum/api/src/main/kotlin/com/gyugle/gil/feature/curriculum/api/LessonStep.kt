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

import com.gyugle.gil.feature.curriculum.api.StudyItemId

/**
 * One step of a lesson. Korean text is plain except that English inside it is wrapped in
 * backticks. English text uses the typographic apostrophe (’).
 *
 * [item] on a graded step is the study item the answer shows the learner knows, if any. It can be
 * an item from an earlier lesson as well as one this lesson introduces.
 */
sealed interface LessonStep {
    /** A few Korean paragraphs, then example sentences to listen to. */
    data class Explain(val paragraphs: List<String>, val examples: List<Sentence>) : LessonStep

    /** A sentence heard before its translation is shown. */
    data class Listen(val sentence: Sentence) : LessonStep

    /** Shadowing: listen, record, and compare. The learner judges it, so it is not graded. */
    data class Shadow(val sentence: Sentence) : LessonStep

    /** A step with a right answer. */
    sealed interface Graded : LessonStep {
        val item: StudyItemId?
    }

    /** An English [prompt], answered by picking its meaning. */
    data class ChooseMeaning(val prompt: Sentence, val options: List<Choice>, override val item: StudyItemId?) : Graded

    /** [audio] is only heard, never shown, until the answer. */
    data class ListenChoose(val audio: Sentence, val options: List<Choice>, override val item: StudyItemId?) : Graded

    /** A Korean prompt, answered by picking the English. */
    data class ChooseEnglish(val ko: String, val options: List<Choice>, override val item: StudyItemId?) : Graded

    /**
     * Builds [answer] from tiles. [tiles] is the answer split up in order, [accepted] holds other
     * right orders of the same tiles, and [extra] tiles belong to no answer.
     */
    data class Arrange(
        val ko: String,
        val answer: Sentence,
        val tiles: List<String>,
        val accepted: List<List<String>>,
        val extra: List<String>,
        override val item: StudyItemId?
    ) : Graded

    /**
     * [text] has `___` where the answer goes. With [options] the learner picks one; without them
     * the learner types one of [answers]. [sentence] is [text] with the first answer filled in.
     */
    data class FillBlank(
        val text: String,
        val ko: String,
        val answers: List<String>,
        val options: List<Choice>,
        val sentence: Sentence,
        override val item: StudyItemId?
    ) : Graded

    /** Hears a word and its meaning, then types [answer]. */
    data class Spell(val answer: String, val ko: String, val ipa: String, override val item: StudyItemId) : Graded

    /** Hears [sentence] and types it. */
    data class Dictation(val sentence: Sentence, override val item: StudyItemId?) : Graded

    /** Hears one of [words] and picks it. [answer] is its index. */
    data class MinimalPair(
        val words: List<PairWord>,
        val answer: Int,
        val why: String?,
        override val item: StudyItemId?
    ) : Graded

    /** A short text and questions about it, answered one after another. */
    data class ReadPassage(val sentences: List<Sentence>, val questions: List<PassageQuestion>) : Graded {
        override val item: StudyItemId? get() = null
    }

    /** Hears [lines], then picks the reply or answers [question]. */
    data class Dialogue(
        val lines: List<DialogueLine>,
        val question: String?,
        val options: List<Choice>,
        override val item: StudyItemId?
    ) : Graded
}
