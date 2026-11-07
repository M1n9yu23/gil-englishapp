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

enum class PartOfSpeech(private val key: String) {
    NOUN("noun"),
    VERB("verb"),
    ADJECTIVE("adjective"),
    ADVERB("adverb"),
    PREPOSITION("preposition"),
    PRONOUN("pronoun"),
    CONJUNCTION("conjunction"),
    INTERJECTION("interjection"),
    MODAL("modal"),
    ARTICLE("article"),
    DETERMINER("determiner"),
    NUMBER("number");

    companion object {
        /** The part of speech the content names [key], such as `noun`, or null for an unknown one. */
        fun fromKey(key: String): PartOfSpeech? = entries.firstOrNull { it.key == key }
    }
}
