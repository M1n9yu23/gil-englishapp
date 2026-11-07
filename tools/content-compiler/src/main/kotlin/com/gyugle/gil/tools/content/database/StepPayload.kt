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
package com.gyugle.gil.tools.content.database

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The JSON stored in `lesson_step.payload`. The app decodes the same shape, so a change here needs a
 * matching change in the app's step models.
 *
 * English text uses the typographic apostrophe. [SentenceRef.id] points at the `sentence` table,
 * which holds the word links for the sentence breakdown sheet.
 */
@Serializable
sealed interface StepPayload {
    @Serializable
    @SerialName("explain")
    data class Explain(
        /** Korean paragraphs. English inside them is wrapped in backticks. */
        val paragraphs: List<String>,
        val examples: List<SentenceRef>
    ) : StepPayload

    @Serializable
    @SerialName("listen")
    data class Listen(val sentence: SentenceRef) : StepPayload

    @Serializable
    @SerialName("chooseMeaning")
    data class ChooseMeaning(val prompt: SentenceRef, val options: List<Option>, val item: String? = null) : StepPayload

    @Serializable
    @SerialName("listenChoose")
    data class ListenChoose(val audio: SentenceRef, val options: List<Option>, val item: String? = null) : StepPayload

    @Serializable
    @SerialName("chooseEnglish")
    data class ChooseEnglish(val ko: String, val options: List<Option>, val item: String? = null) : StepPayload

    @Serializable
    @SerialName("arrange")
    data class Arrange(
        val ko: String,
        val answer: SentenceRef,
        /** The answer split into tiles, in order. */
        val tiles: List<String>,
        /** Other correct orders of the same tiles. */
        val accepted: List<List<String>>,
        val extra: List<String>,
        val item: String? = null
    ) : StepPayload

    @Serializable
    @SerialName("fillBlank")
    data class FillBlank(
        /** The sentence with `___` where the answer goes. */
        val text: String,
        val ko: String,
        /** Typed answers that count as right. Empty when [options] are given. */
        val answers: List<String>,
        val options: List<Option>,
        /** The sentence with the first right answer filled in, for playback. */
        val sentence: SentenceRef,
        val item: String? = null
    ) : StepPayload

    @Serializable
    @SerialName("spell")
    data class Spell(val item: String, val wordId: String, val answer: String, val ko: String, val ipa: String) :
        StepPayload

    @Serializable
    @SerialName("dictation")
    data class Dictation(val sentence: SentenceRef, val item: String? = null) : StepPayload

    @Serializable
    @SerialName("minimalPair")
    data class MinimalPair(
        val words: List<PairWord>,
        /** Index into [words] of the word that is played. */
        val answer: Int,
        val why: String? = null,
        val item: String? = null
    ) : StepPayload

    @Serializable
    @SerialName("shadow")
    data class Shadow(val sentence: SentenceRef) : StepPayload

    @Serializable
    @SerialName("readPassage")
    data class ReadPassage(val sentences: List<SentenceRef>, val questions: List<Question>) : StepPayload

    @Serializable
    @SerialName("dialogue")
    data class Dialogue(
        val lines: List<Line>,
        val question: String? = null,
        val options: List<Option>,
        val item: String? = null
    ) : StepPayload
}

@Serializable
data class SentenceRef(val id: Long, val text: String, val ko: String? = null)

/** An answer choice. [english] tells the app to draw it as English. */
@Serializable
data class Option(val text: String, val english: Boolean, val correct: Boolean, val why: String? = null)

@Serializable
data class PairWord(val text: String, val ipa: String)

@Serializable
data class Question(val ko: String, val options: List<Option>)

@Serializable
data class Line(val speaker: String, val sentence: SentenceRef)

/** A word of a sentence linked to the lexicon, stored in `sentence.tokens`. */
@Serializable
data class TokenJson(val start: Int, val end: Int, val word: String? = null, val sense: String? = null)

/** A chunk of a sentence and its role, stored in `sentence.chunks`. */
@Serializable
data class ChunkJson(val start: Int, val end: Int, val role: String)

@OptIn(ExperimentalSerializationApi::class)
val PayloadJson = Json {
    classDiscriminator = "type"
    explicitNulls = false
    encodeDefaults = false
}
