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

/** A word of a sentence as it was written, and the lowercase form compared. */
internal data class Token(val text: String, val key: String)

/** Typographic quotes become ASCII, runs of spaces become one, and the ends are trimmed. */
internal fun String.normalizeTyped(): String = replace('’', '\'').replace('‘', '\'').trim().replace(Regex("\\s+"), " ")

/**
 * The words of [text], without the punctuation around them. An apostrophe or hyphen inside a word
 * stays (I’m, twenty-one); everything else that is not a letter or digit separates words.
 */
internal fun tokenize(text: String): List<Token> {
    // Same length as text, so each match also marks the word in the original.
    val plain = text.replace('’', '\'').replace('‘', '\'')
    return WORD.findAll(plain)
        .map { Token(text = text.substring(it.range), key = it.value.lowercase()) }
        .toList()
}

private val WORD = Regex("[\\p{L}\\p{N}]+(?:['-][\\p{L}\\p{N}]+)*")

/**
 * Edit distance where swapping two neighbouring letters counts as one edit, as a typo usually is.
 * Counted in code points, so a letter outside the BMP is one letter.
 */
internal fun editDistance(a: String, b: String): Int {
    val x = a.codePoints().toArray()
    val y = b.codePoints().toArray()
    val d = Array(x.size + 1) { IntArray(y.size + 1) }
    for (i in 0..x.size) d[i][0] = i
    for (j in 0..y.size) d[0][j] = j
    for (i in 1..x.size) {
        for (j in 1..y.size) {
            val cost = if (x[i - 1] == y[j - 1]) 0 else 1
            d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost)
            if (i > 1 && j > 1 && x[i - 1] == y[j - 2] && x[i - 2] == y[j - 1]) {
                d[i][j] = minOf(d[i][j], d[i - 2][j - 2] + 1)
            }
        }
    }
    return d[x.size][y.size]
}

/** Which elements of [a] belong to a longest common subsequence with [b]. */
internal fun <T> commonSubsequenceMask(a: List<T>, b: List<T>): BooleanArray {
    val lengths = Array(a.size + 1) { IntArray(b.size + 1) }
    for (i in a.indices.reversed()) {
        for (j in b.indices.reversed()) {
            lengths[i][j] = if (a[i] == b[j]) lengths[i + 1][j + 1] + 1 else maxOf(lengths[i + 1][j], lengths[i][j + 1])
        }
    }
    val mask = BooleanArray(a.size)
    var i = 0
    var j = 0
    while (i < a.size && j < b.size) {
        when {
            a[i] == b[j] -> {
                mask[i] = true
                i++
                j++
            }

            lengths[i + 1][j] >= lengths[i][j + 1] -> i++

            else -> j++
        }
    }
    return mask
}

/**
 * Below this many letters a one-letter slip often spells another word (is, it), so only longer
 * words get the benefit of the doubt.
 */
internal const val MIN_LENGTH_FOR_CLOSE = 4
