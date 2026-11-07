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

/** Parts of speech. [abbreviation] is the third part of a sense ID: `sense.run.v.move-fast`. */
@Serializable
enum class PartOfSpeech(val abbreviation: String) {
    @SerialName("noun")
    NOUN("n"),

    @SerialName("verb")
    VERB("v"),

    @SerialName("adjective")
    ADJECTIVE("adj"),

    @SerialName("adverb")
    ADVERB("adv"),

    @SerialName("preposition")
    PREPOSITION("prep"),

    @SerialName("pronoun")
    PRONOUN("pron"),

    @SerialName("conjunction")
    CONJUNCTION("conj"),

    @SerialName("interjection")
    INTERJECTION("interj"),

    @SerialName("modal")
    MODAL("modal"),

    @SerialName("article")
    ARTICLE("art"),

    @SerialName("determiner")
    DETERMINER("det"),

    @SerialName("number")
    NUMBER("num")
    ;

    /** The YAML and database spelling, such as `noun`. */
    val key: String get() = name.lowercase()
}
