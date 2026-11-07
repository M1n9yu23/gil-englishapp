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
package com.gyugle.gil.feature.review.impl.domain

import com.gyugle.gil.feature.review.impl.domain.model.AnswerGrade

/**
 * A typed word or phrase against [answer]. Case, punctuation at either end and extra spaces do not
 * count, and curly apostrophes match straight ones. One letter off, or two neighbouring letters
 * swapped, is [AnswerGrade.CLOSE] in an answer of four letters or more; in a shorter one a slip
 * often spells another word (is, it), so it is wrong.
 *
 * The same rule as a lesson's spelling step. Review keeps its own copy because it owns its cards.
 */
fun gradeTyped(typed: String, answer: String): AnswerGrade {
    val written = typed.normalized().lowercase()
    val expected = answer.normalized().lowercase()
    return when {
        written == expected -> AnswerGrade.CORRECT
        expected.length >= MIN_LENGTH_FOR_CLOSE && editDistance(written, expected) == 1 -> AnswerGrade.CLOSE
        else -> AnswerGrade.WRONG
    }
}

private const val MIN_LENGTH_FOR_CLOSE = 4

private const val END_PUNCTUATION = ".,!?"

private val SPACES = Regex("\\s+")

private fun String.normalized(): String = replace('’', '\'')
    .replace('‘', '\'')
    .trim { it.isWhitespace() || it in END_PUNCTUATION }
    .replace(SPACES, " ")

/** Edit distance where swapping two neighbouring letters counts as one edit. */
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
