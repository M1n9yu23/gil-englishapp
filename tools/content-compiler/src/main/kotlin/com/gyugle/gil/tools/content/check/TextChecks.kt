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
package com.gyugle.gil.tools.content.check

import com.gyugle.gil.tools.content.font.FontCoverage

/** Character limits that keep text from wrapping badly on a phone. */
object Limits {
    const val TILE = 20
    const val OPTION = 40
    const val PARAGRAPH = 200
}

/**
 * Checks single strings: empty, too long, characters the bundled fonts cannot draw, and style.
 *
 * Korean text is drawn with [uiFont]; English learning text, IPA and anything in backticks inside
 * Korean text with [englishFont]. A null font skips that check.
 */
class TextChecks(
    private val issues: Issues,
    private val uiFont: FontCoverage?,
    private val englishFont: FontCoverage?
) {

    /**
     * Korean text. [prose] is explanation-like text where English belongs in backticks; app chrome
     * text in either language uses [prose] = false.
     */
    fun korean(text: String, where: String, limit: Int? = null, prose: Boolean = false) {
        if (!notEmpty(text, where)) return
        length(text, where, limit)
        if (text.count { it == '`' } % 2 != 0) {
            issues.add(Rule.MARKUP_INVALID, where, "a backtick is not closed")
            return
        }
        text.split('`').forEachIndexed { index, part ->
            val english = index % 2 == 1
            glyphs(part, where, if (english) englishFont else uiFont)
            if (prose && !english && LATIN.containsMatchIn(part)) {
                issues.add(
                    Rule.TEXT_LATIN,
                    where,
                    "put English inside backticks so it is shown as English: '${LATIN.find(part)!!.value}'"
                )
            }
        }
        BANNED_KO.firstOrNull { it.first in text }?.let { (phrase, advice) ->
            issues.add(Rule.STYLE_KO, where, "'$phrase': $advice")
        }
        PERIOD_BEFORE_PARTICLE.findAll(text)
            .firstOrNull { it.groupValues[1].substringAfterLast(' ') !in ABBREVIATIONS }
            ?.let {
                issues.add(
                    Rule.STYLE_KO,
                    where,
                    "drop the period before a Korean particle: '${it.value}' -> '`${it.groupValues[1]}`${it.groupValues[2]}'"
                )
            }
    }

    /** App chrome text in English, drawn with the UI font. */
    fun chrome(text: String, where: String) {
        if (!notEmpty(text, where)) return
        glyphs(text, where, uiFont)
    }

    /**
     * English learning text. [sentence] asks for a capital start and closing punctuation; [spoken]
     * means text-to-speech reads it aloud.
     */
    fun english(text: String, where: String, limit: Int? = null, sentence: Boolean = false, spoken: Boolean = true) {
        if (!notEmpty(text, where)) return
        length(text, where, limit)
        glyphs(text, where, englishFont)
        BANNED_EN.firstOrNull { it.first in text }?.let { (phrase, advice) ->
            issues.add(Rule.STYLE_EN, where, "'$phrase': $advice")
        }
        if (sentence) {
            val first = text.trimStart('"').firstOrNull()
            if (first != null && first.isLetter() && !first.isUpperCase()) {
                issues.add(Rule.STYLE_EN, where, "a sentence starts with a capital letter")
            }
            if (text.trimEnd('"').lastOrNull() !in SENTENCE_END) {
                issues.add(Rule.STYLE_EN, where, "a sentence ends with . ? or !")
            }
        }
        if (spoken) {
            TTS_SYMBOLS.firstOrNull { it in text }?.let {
                issues.add(Rule.TTS_SYMBOL, where, "text-to-speech may misread '$it'; write it as a word")
            }
            ABBREVIATION.find(text)?.let {
                issues.add(Rule.TTS_SYMBOL, where, "text-to-speech may misread the abbreviation '${it.value}'")
            }
        }
    }

    fun ipa(text: String, where: String) {
        if (!notEmpty(text, where)) return
        glyphs(text, where, englishFont)
    }

    private fun notEmpty(text: String, where: String): Boolean {
        if (text.isNotBlank()) return true
        issues.add(Rule.TEXT_EMPTY, where, "the text is empty")
        return false
    }

    private fun length(text: String, where: String, limit: Int?) {
        val count = text.codePointCount(0, text.length)
        if (limit != null && count > limit) {
            issues.add(Rule.TEXT_LENGTH, where, "$count characters, the limit is $limit: '$text'")
        }
    }

    private fun glyphs(text: String, where: String, font: FontCoverage?) {
        if (font == null) return
        val missing = text.codePoints().toArray().distinct()
            .filter { !Character.isWhitespace(it) && !font.covers(it) }
        if (missing.isNotEmpty()) {
            val shown = missing.joinToString(" ") { "'${String(Character.toChars(it))}' U+%04X".format(it) }
            issues.add(Rule.FONT_GLYPH, where, "the bundled font cannot draw $shown")
        }
    }

    private companion object {
        val LATIN = Regex("[A-Za-z]+")
        val SENTENCE_END = setOf('.', '?', '!')
        val TTS_SYMBOLS = listOf("&", "%", "/", "#", "+", "=", "*")

        /** Two or more capitals in a row, such as TV or USA. `I` and `OK` are read correctly. */
        val ABBREVIATION = Regex("\\b(?!OK\\b)[A-Z]{2,}\\b")

        /**
         * "`Hello.`는": an English sentence in backticks whose period is followed by a particle.
         * Korean may leave out the period of a quoted sentence, and without quotation marks the
         * period reads like the end of the Korean sentence. ? and ! stay.
         */
        val PERIOD_BEFORE_PARTICLE = Regex("`([^`]*[A-Za-z])\\.`([가-힣])")
        val ABBREVIATIONS = setOf("Mr", "Mrs", "Ms", "Dr", "St", "a.m", "p.m", "etc")

        val BANNED_KO = listOf(
            "성공적으로" to "빼거나 결과를 바로 쓰세요",
            "되요" to "'돼요'로 쓰세요",
            "할께요" to "'할게요'로 쓰세요",
            "몇일" to "'며칠'로 쓰세요",
            "아니예요" to "'아니에요'로 쓰세요",
            "에 있어서" to "번역투예요",
            "되어지" to "이중 피동이에요",
            "당신" to "주어를 빼세요",
            "여러분" to "주어를 빼세요",
            "!!" to "느낌표는 하나만"
        )

        val BANNED_EN = listOf(
            "—" to "use a period, comma or colon instead of an em dash",
            "!!" to "use one exclamation mark",
            "  " to "use one space"
        )
    }
}
