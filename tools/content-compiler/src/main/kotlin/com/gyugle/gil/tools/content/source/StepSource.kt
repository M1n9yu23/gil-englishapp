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
package com.gyugle.gil.tools.content.source

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One step of a lesson. The `type` key in YAML picks the subclass.
 *
 * [checkVocab] turns off the vocabulary check for the step. Sound and letter drills need it, and so
 * do the first lessons that show English before the learner knows any words.
 */
@Serializable
sealed interface StepSource {
    val checkVocab: Boolean

    /** The study item this step tests, so the lesson can grade it. */
    val item: String?
}

@Serializable
@SerialName("explain")
data class ExplainSource(
    /** Korean paragraphs separated by blank lines. English inside the text goes in backticks. */
    val text: String,
    val examples: List<ExampleSource> = emptyList(),
    override val checkVocab: Boolean = true
) : StepSource {
    override val item: String? get() = null
}

@Serializable
@SerialName("listen")
data class ListenSource(
    val en: String,
    val ko: String,
    val literal: String? = null,
    val grammar: String? = null,
    override val checkVocab: Boolean = true
) : StepSource {
    override val item: String? get() = null
}

@Serializable
@SerialName("chooseMeaning")
data class ChooseMeaningSource(
    val en: String,
    val options: List<OptionSource>,
    override val item: String? = null,
    override val checkVocab: Boolean = true
) : StepSource

@Serializable
@SerialName("listenChoose")
data class ListenChooseSource(
    val en: String,
    val options: List<OptionSource>,
    override val item: String? = null,
    override val checkVocab: Boolean = true
) : StepSource

@Serializable
@SerialName("chooseEnglish")
data class ChooseEnglishSource(
    val ko: String,
    val options: List<OptionSource>,
    override val item: String? = null,
    override val checkVocab: Boolean = true
) : StepSource

@Serializable
@SerialName("arrange")
data class ArrangeSource(
    val ko: String,
    val en: String,
    /** Other word orders that are also right. They must use the same tiles. */
    val accept: List<String> = emptyList(),
    /** Distractor tiles, at most two. */
    val extra: List<String> = emptyList(),
    override val item: String? = null,
    override val checkVocab: Boolean = true
) : StepSource

@Serializable
@SerialName("fillBlank")
data class FillBlankSource(
    /** The sentence with one blank written as `___`. */
    val en: String,
    val ko: String,
    /** Accepted typed answers. Leave empty when [options] are given. */
    val answers: List<String> = emptyList(),
    val options: List<OptionSource> = emptyList(),
    override val item: String? = null,
    override val checkVocab: Boolean = true
) : StepSource

@Serializable
@SerialName("spell")
data class SpellSource(val sense: String) : StepSource {
    override val item: String get() = sense
    override val checkVocab: Boolean get() = true
}

@Serializable
@SerialName("dictation")
data class DictationSource(
    val en: String,
    val ko: String,
    override val item: String? = null,
    override val checkVocab: Boolean = true
) : StepSource

@Serializable
@SerialName("minimalPair")
data class MinimalPairSource(
    val words: List<String>,
    /** The word that is played. */
    val answer: String,
    val why: String? = null,
    override val item: String? = null
) : StepSource {
    override val checkVocab: Boolean get() = false
}

@Serializable
@SerialName("shadow")
data class ShadowSource(val en: String, val ko: String, override val checkVocab: Boolean = true) : StepSource {
    override val item: String? get() = null
}

@Serializable
@SerialName("readPassage")
data class ReadPassageSource(
    val sentences: List<ExampleSource>,
    val questions: List<QuestionSource>,
    override val checkVocab: Boolean = true
) : StepSource {
    override val item: String? get() = null
}

@Serializable
data class QuestionSource(val ko: String, val options: List<OptionSource>)

@Serializable
@SerialName("dialogue")
data class DialogueSource(
    val lines: List<LineSource>,
    /** The Korean question above the reply choices. The app has a default. */
    val question: String? = null,
    val options: List<OptionSource>,
    override val item: String? = null,
    override val checkVocab: Boolean = true
) : StepSource

@Serializable
data class LineSource(val speaker: String, val en: String, val ko: String)
