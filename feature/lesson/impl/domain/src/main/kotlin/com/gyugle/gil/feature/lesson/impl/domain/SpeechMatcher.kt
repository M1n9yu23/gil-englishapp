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
package com.gyugle.gil.feature.lesson.impl.domain

import com.gyugle.gil.feature.lesson.impl.domain.model.MarkedWord

/**
 * Compares what a speech recognizer heard with the sentence the learner shadowed.
 *
 * A recognizer writes the same speech in more than one way: "I'm" or "I am", "ten" or "10". Both
 * sides are brought to one form first, so the learner is not marked wrong for the recognizer's
 * spelling. Words are matched in order, so a skipped word stays unmatched.
 */
object SpeechMatcher {
    /** Each word of [target] as written, marked by whether [heard] contains it in order. */
    fun match(target: String, heard: String): List<MarkedWord> {
        val targetWords = tokenize(target).map { it.text to canonicalWords(it.key) }
        val heardKeys = tokenize(heard).flatMap { canonicalWords(it.key) }
        val matched = commonSubsequenceMask(targetWords.flatMap { it.second }, heardKeys)
        var next = 0
        return targetWords.map { (text, keys) ->
            val range = next until next + keys.size
            next += keys.size
            MarkedWord(text, range.all { matched[it] })
        }
    }

    /** The words [word] stands for: a contraction opens up and a number is spelled out. */
    internal fun canonicalWords(word: String): List<String> {
        val key = word.lowercase().replace('’', '\'')
        IRREGULAR[key]?.let { return it }
        key.toIntOrNull()?.let { number -> if (number in 0..999) return spellNumber(number) }
        if ('-' in key) return key.split('-').flatMap(::canonicalWords)
        SUFFIXES.forEach { (suffix, expansion) ->
            if (key.endsWith(suffix) && key.length > suffix.length) {
                return canonicalWords(key.dropLast(suffix.length)) + expansion
            }
        }
        // 's is "is" after a pronoun or question word; elsewhere it is usually a possessive.
        if (key.endsWith("'s") && key.dropLast(2) in IS_BEFORE) return listOf(key.dropLast(2), "is")
        return listOf(key)
    }

    private val IRREGULAR = mapOf(
        "can't" to listOf("can", "not"),
        "cannot" to listOf("can", "not"),
        "won't" to listOf("will", "not"),
        "shan't" to listOf("shall", "not"),
        "let's" to listOf("let", "us"),
        "okay" to listOf("ok")
    )

    private val SUFFIXES = listOf(
        "n't" to "not",
        "'re" to "are",
        "'m" to "am",
        "'ll" to "will",
        "'ve" to "have",
        "'d" to "would"
    )

    private val IS_BEFORE = setOf(
        "he", "she", "it", "that", "this", "there", "here", "what", "where", "who", "how", "when", "why"
    )

    private val ONES = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"
    )
    private val TENS = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    private fun spellNumber(number: Int): List<String> = when {
        number < 20 -> listOf(ONES[number])
        number < 100 -> listOfNotNull(TENS[number / 10], (number % 10).takeIf { it > 0 }?.let { ONES[it] })
        else -> listOf(ONES[number / 100], "hundred") + (number % 100).takeIf { it > 0 }?.let(::spellNumber).orEmpty()
    }
}
