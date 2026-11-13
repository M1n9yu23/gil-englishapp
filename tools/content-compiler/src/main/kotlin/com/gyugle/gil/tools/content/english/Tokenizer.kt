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

/** A word in a sentence. [start] and [end] are UTF-16 offsets, end exclusive. */
data class Token(val start: Int, val end: Int, val text: String) {
    /** Lowercase with the typographic apostrophe folded to `'`, the form used for lookups. */
    val key: String get() = normalizeEnglish(text)

    val isNumber: Boolean get() = text.first().isDigit()
}

object Tokenizer {

    /**
     * Splits [text] into words. Apostrophes and hyphens stay inside a word when letters are on
     * both sides (`don't`, `twenty-one`); digits may carry `,` and `.` (`1,000`, `3.5`).
     */
    fun tokenize(text: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < text.length) {
            if (!text[i].isLetterOrDigit()) {
                i++
                continue
            }
            val start = i
            i++
            while (i < text.length) {
                val c = text[i]
                val joins = c.isLetterOrDigit() ||
                    (c in WORD_JOINERS && text.getOrNull(i + 1)?.isLetter() == true && text[i - 1].isLetter()) ||
                    (c in NUMBER_JOINERS && text.getOrNull(i + 1)?.isDigit() == true && text[i - 1].isDigit())
                if (!joins) break
                i++
            }
            tokens += Token(start, i, text.substring(start, i))
        }
        return tokens
    }

    private val WORD_JOINERS = setOf('\'', '’', '-')
    private val NUMBER_JOINERS = setOf(',', '.')
}

/** Lowercases and folds the typographic apostrophe so `I’m` and `I'm` look up the same entry. */
fun normalizeEnglish(text: String): String = text.replace('’', '\'').lowercase()

/** Replaces the typing apostrophe with the typographic one for display. */
fun typographic(text: String): String = text.replace('\'', '’')

/**
 * [typographic] for text that is not all English, such as titles in either language: only an
 * apostrophe between two letters changes, so a quote mark around a word stays as written.
 */
fun apostrophes(text: String): String = BETWEEN_LETTERS.replace(text, "’")

private val BETWEEN_LETTERS = Regex("(?<=\\p{L})'(?=\\p{L})")
