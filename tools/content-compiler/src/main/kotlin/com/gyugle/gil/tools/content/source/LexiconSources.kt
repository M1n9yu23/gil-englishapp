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

/** One headword in a `content/lexicon/` file. */
@Serializable
data class WordSource(
    val word: String,
    val id: String,
    /** American IPA. Overrides CMUdict, which picks the first of several pronunciations. */
    val ipa: String? = null,
    /** Hangul pronunciation aid for the first stages. */
    val hangul: String? = null,
    val forms: FormsSource = FormsSource(),
    val senses: List<SenseSource>,
    val family: List<String> = emptyList(),
    val collocations: List<CollocationSource> = emptyList(),
    val notes: List<NoteSource> = emptyList()
)

/** Irregular forms only. Regular forms are derived from the word and its parts of speech. */
@Serializable
data class FormsSource(
    val plural: String? = null,
    val third: String? = null,
    val past: String? = null,
    val pastParticiple: String? = null,
    val ing: String? = null,
    val comparative: String? = null,
    val superlative: String? = null
)

@Serializable
data class SenseSource(
    val id: String,
    val pos: PartOfSpeech,
    val ko: String,
    /** Simple English definition, from B1 on. */
    val definition: String? = null,
    /** Stage code for a sense no lesson introduces yet, such as `b1`. */
    val level: String? = null,
    /** IPA for this sense when it differs from the word (read, lead). */
    val ipa: String? = null,
    val examples: List<ExampleSource> = emptyList()
)

@Serializable
data class CollocationSource(val en: String, val ko: String)

@Serializable
data class NoteSource(val kind: NoteKind, val ko: String)

@Serializable
enum class NoteKind {
    @SerialName("konglish")
    KONGLISH,

    @SerialName("mistake")
    MISTAKE,

    @SerialName("usage")
    USAGE
}

/** `content/patterns.yaml`: chunks such as "I'd like ~". */
@Serializable
data class PatternSource(
    val id: String,
    val text: String,
    val ko: String,
    val note: String? = null,
    val level: String? = null,
    val examples: List<ExampleSource> = emptyList()
)

/** `content/sounds.yaml`: the sound items of the first stages. */
@Serializable
data class SoundSource(
    val id: String,
    /** IPA symbol without slashes. */
    val symbol: String,
    /** Letters that usually spell the sound, such as `f, ph`. */
    val spelling: String,
    /** How to make the sound, in Korean. */
    val ko: String,
    val level: String? = null,
    val examples: List<String> = emptyList()
)

/** One note in a `content/grammar/` file. */
@Serializable
data class GrammarSource(
    val id: String,
    val title: String,
    val body: String,
    val examples: List<ExampleSource> = emptyList()
)
