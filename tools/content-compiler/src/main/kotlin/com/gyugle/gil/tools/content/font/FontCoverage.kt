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
package com.gyugle.gil.tools.content.font

import java.io.File
import java.nio.ByteBuffer
import java.util.BitSet

/**
 * The characters every file of a font family can draw. A character missing from the bundled font
 * would fall back to a system font and look out of place, so content may not use it.
 */
class FontCoverage private constructor(private val codePoints: BitSet) {

    fun covers(codePoint: Int): Boolean = codePoints[codePoint]

    companion object {
        /** Reads the Unicode cmap of each TrueType or OpenType file and keeps the intersection. */
        fun of(files: List<File>): FontCoverage {
            require(files.isNotEmpty()) { "no font files" }
            val sets = files.map { readCmap(ByteBuffer.wrap(it.readBytes()), it.name) }
            val result = sets.first().clone() as BitSet
            sets.drop(1).forEach { result.and(it) }
            return FontCoverage(result)
        }

        private fun readCmap(font: ByteBuffer, name: String): BitSet {
            val tableCount = font.getShort(4).toInt() and 0xFFFF
            val cmap = (0 until tableCount).map { 12 + it * 16 }
                .firstOrNull { font.getInt(it) == CMAP_TAG }
                ?.let { font.getInt(it + 8) }
                ?: error("$name has no cmap table")
            val subtableCount = font.getShort(cmap + 2).toInt() and 0xFFFF
            val subtables = (0 until subtableCount).map { k ->
                val record = cmap + 4 + k * 8
                Triple(font.getShort(record).toInt(), font.getShort(record + 2).toInt(), cmap + font.getInt(record + 4))
            }
            val chosen = subtables.firstOrNull { it.first == 3 && it.second == 10 }
                ?: subtables.firstOrNull { it.first == 3 && it.second == 1 }
                ?: subtables.firstOrNull { it.first == 0 }
                ?: error("$name has no Unicode cmap")
            val offset = chosen.third
            return when (val format = font.getShort(offset).toInt()) {
                4 -> readFormat4(font, offset)
                12 -> readFormat12(font, offset)
                else -> error("$name uses cmap format $format, which the compiler does not read")
            }
        }

        private fun readFormat4(font: ByteBuffer, offset: Int): BitSet {
            val set = BitSet()
            val segments = (font.getShort(offset + 6).toInt() and 0xFFFF) / 2
            val ends = offset + 14
            val starts = ends + segments * 2 + 2
            val deltas = starts + segments * 2
            val rangeOffsets = deltas + segments * 2
            for (s in 0 until segments) {
                val end = font.getShort(ends + s * 2).toInt() and 0xFFFF
                val start = font.getShort(starts + s * 2).toInt() and 0xFFFF
                val delta = font.getShort(deltas + s * 2).toInt()
                val rangeOffsetAt = rangeOffsets + s * 2
                val rangeOffset = font.getShort(rangeOffsetAt).toInt() and 0xFFFF
                if (start == 0xFFFF) continue
                for (c in start..end) {
                    val glyph = if (rangeOffset == 0) {
                        (c + delta) and 0xFFFF
                    } else {
                        val raw = font.getShort(rangeOffsetAt + rangeOffset + (c - start) * 2).toInt() and 0xFFFF
                        if (raw == 0) 0 else (raw + delta) and 0xFFFF
                    }
                    if (glyph != 0) set.set(c)
                }
            }
            return set
        }

        private fun readFormat12(font: ByteBuffer, offset: Int): BitSet {
            val set = BitSet()
            val groups = font.getInt(offset + 12)
            for (g in 0 until groups) {
                val group = offset + 16 + g * 12
                val start = font.getInt(group)
                val end = font.getInt(group + 4)
                val firstGlyph = font.getInt(group + 8)
                for (c in start..end) {
                    if (firstGlyph + (c - start) != 0) set.set(c)
                }
            }
            return set
        }

        private const val CMAP_TAG = 0x636D6170
    }
}
