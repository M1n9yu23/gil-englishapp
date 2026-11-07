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
package com.gyugle.gil.tools.content.english

/**
 * A contraction split into its base word and the words its ending can stand for. `I'd` is `I` plus
 * `would` or `had`; `Mina's` can also be a possessive.
 */
data class Contraction(val base: String, val alternatives: List<String>, val canBePossessive: Boolean)

object Contractions {

    /** Splits a lowercase key with a straight apostrophe, or returns null when it is no contraction. */
    fun split(key: String): Contraction? {
        SPECIAL[key]?.let { return it }
        for ((suffix, alternatives) in SUFFIXES) {
            if (key.length > suffix.length && key.endsWith(suffix)) {
                return Contraction(key.dropLast(suffix.length), alternatives, canBePossessive = suffix == "'s")
            }
        }
        return null
    }

    private val SPECIAL = mapOf(
        "can't" to Contraction("can", listOf("not"), canBePossessive = false),
        "won't" to Contraction("will", listOf("not"), canBePossessive = false),
        "shan't" to Contraction("shall", listOf("not"), canBePossessive = false),
        "let's" to Contraction("let", listOf("us"), canBePossessive = false)
    )

    private val SUFFIXES = listOf(
        "n't" to listOf("not"),
        "'m" to listOf("am"),
        "'re" to listOf("are"),
        "'ve" to listOf("have"),
        "'ll" to listOf("will"),
        "'d" to listOf("would", "had"),
        "'s" to listOf("is", "has")
    )
}
