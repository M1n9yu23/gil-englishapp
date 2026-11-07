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
 * An English sentence with its markup removed.
 *
 * Markup in the source:
 * - `{runs|sense.run.v.manage}` says which sense a word has in this sentence.
 * - `[every morning]{when}` marks a chunk and its role for the sentence breakdown.
 * - `@Mina` marks a proper noun, which the vocabulary check skips.
 *
 * Offsets are UTF-16 indexes into [text], end exclusive.
 */
data class MarkedSentence(
    val text: String,
    val senseMarks: List<SenseMark>,
    val chunks: List<Chunk>,
    val properNouns: List<IntRange>
)

data class SenseMark(val start: Int, val end: Int, val senseId: String)

data class Chunk(val start: Int, val end: Int, val role: ChunkRole)

enum class ChunkRole(val key: String) {
    WHO("who"),
    DO("do"),
    WHAT("what"),
    WHERE("where"),
    WHEN("when")
}

sealed interface MarkupResult {
    data class Parsed(val sentence: MarkedSentence) : MarkupResult

    data class Invalid(val reason: String) : MarkupResult
}

object SentenceMarkup {

    fun parse(source: String): MarkupResult {
        val text = StringBuilder()
        val marks = mutableListOf<SenseMark>()
        val chunks = mutableListOf<Chunk>()
        val properNouns = mutableListOf<IntRange>()
        var chunkStart = -1
        var i = 0
        while (i < source.length) {
            when (val c = source[i]) {
                '[' -> {
                    if (chunkStart >= 0) return MarkupResult.Invalid("a chunk starts inside another chunk")
                    chunkStart = text.length
                    i++
                }

                ']' -> {
                    if (chunkStart < 0) return MarkupResult.Invalid("']' without '['")
                    val close = source.indexOf('}', i)
                    if (source.getOrNull(i + 1) != '{' || close < 0) {
                        return MarkupResult.Invalid("a chunk needs a role right after it, like [I]{who}")
                    }
                    val roleKey = source.substring(i + 2, close)
                    val role = ChunkRole.entries.firstOrNull { it.key == roleKey }
                        ?: return MarkupResult.Invalid(
                            "unknown chunk role '$roleKey', use one of ${ChunkRole.entries.joinToString { it.key }}"
                        )
                    if (chunkStart == text.length) return MarkupResult.Invalid("empty chunk")
                    chunks += Chunk(chunkStart, text.length, role)
                    chunkStart = -1
                    i = close + 1
                }

                '{' -> {
                    val close = source.indexOf('}', i)
                    val body = if (close < 0) "" else source.substring(i + 1, close)
                    val bar = body.indexOf('|')
                    if (close < 0 || bar <= 0 || bar == body.lastIndex || body.any { it in "{[@" }) {
                        return MarkupResult.Invalid("a sense mark looks like {runs|sense.run.v.manage}")
                    }
                    val start = text.length
                    text.append(body, 0, bar)
                    marks += SenseMark(start, text.length, body.substring(bar + 1))
                    i = close + 1
                }

                '}' -> return MarkupResult.Invalid("'}' without '{'")

                '@' -> {
                    val start = text.length
                    var j = i + 1
                    while (j < source.length && (source[j].isLetter() || source[j] == '-')) j++
                    val name = source.substring(i + 1, j)
                    if (name.isEmpty() || !name[0].isUpperCase()) {
                        return MarkupResult.Invalid("a proper noun after '@' starts with a capital letter")
                    }
                    text.append(name)
                    properNouns += start until text.length
                    i = j
                }

                else -> {
                    text.append(c)
                    i++
                }
            }
        }
        if (chunkStart >= 0) return MarkupResult.Invalid("'[' without ']'")
        return MarkupResult.Parsed(MarkedSentence(text.toString(), marks, chunks, properNouns))
    }
}
