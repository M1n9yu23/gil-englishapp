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

import com.gyugle.gil.tools.content.pronunciation.Cmudict
import com.gyugle.gil.tools.content.pronunciation.Ipa

/** Kinds of inflected forms. [key] is the YAML spelling in `forms:` and `inflections:`. */
enum class FormKind(val key: String) {
    PLURAL("plural"),
    THIRD("third"),
    PAST("past"),
    PAST_PARTICIPLE("pastParticiple"),
    ING("ing"),
    COMPARATIVE("comparative"),
    SUPERLATIVE("superlative"),

    /** `'s` after a noun. It has no stored form; the gate in `inflections:` still applies. */
    POSSESSIVE("possessive")
    ;

    companion object {
        fun fromKey(key: String): FormKind? = entries.firstOrNull { it.key == key }
    }
}

/**
 * Regular English inflection. Irregular forms come from the lexicon and win over these. Doubling
 * of the final consonant (stop → stopped, begin → beginning) uses CMUdict stress, so visit →
 * visiting is not doubled.
 */
class Inflector(private val cmudict: Cmudict) {

    /** The regular form of [word], or null when the word has none of that kind. */
    fun regular(word: String, kind: FormKind): String? {
        val w = word.lowercase()
        if (' ' in w || w.length < 2 || !w.all { it.isLetter() }) return null
        return when (kind) {
            FormKind.PLURAL -> sForm(w, verb = false)
            FormKind.THIRD -> sForm(w, verb = true)
            FormKind.PAST, FormKind.PAST_PARTICIPLE -> edForm(w)
            FormKind.ING -> ingForm(w)
            FormKind.COMPARATIVE -> if (gradable(w)) stem(w, "er") else null
            FormKind.SUPERLATIVE -> if (gradable(w)) stem(w, "est") else null
            FormKind.POSSESSIVE -> null
        }
    }

    private fun sForm(w: String, verb: Boolean): String = when {
        w.endsWith("s") || w.endsWith("x") || w.endsWith("z") || w.endsWith("ch") || w.endsWith("sh") -> w + "es"
        w.endsWith("y") && !isVowel(w[w.length - 2]) -> w.dropLast(1) + "ies"
        verb && w.endsWith("o") && !isVowel(w[w.length - 2]) -> w + "es"
        else -> w + "s"
    }

    private fun edForm(w: String): String = when {
        w.endsWith("e") -> w + "d"
        w.endsWith("y") && !isVowel(w[w.length - 2]) -> w.dropLast(1) + "ied"
        doublesFinal(w) -> w + w.last() + "ed"
        else -> w + "ed"
    }

    private fun ingForm(w: String): String = when {
        w.endsWith("ie") -> w.dropLast(2) + "ying"

        w.endsWith("e") && w.length > 2 && !w.endsWith("ee") && !w.endsWith("ye") && !w.endsWith("oe") ->
            w.dropLast(1) + "ing"

        doublesFinal(w) -> w + w.last() + "ing"

        else -> w + "ing"
    }

    /** Adjectives that take -er/-est: one syllable, or two ending in -y. */
    private fun gradable(w: String): Boolean {
        val count = syllables(w).first
        return count == 1 || (count == 2 && w.endsWith("y"))
    }

    private fun stem(w: String, suffix: String): String = when {
        w.endsWith("e") -> w + suffix.drop(1)
        w.endsWith("y") && !isVowel(w[w.length - 2]) -> w.dropLast(1) + "i" + suffix
        doublesFinal(w) -> w + w.last() + suffix
        else -> w + suffix
    }

    private fun doublesFinal(w: String): Boolean {
        if (w.length < 3) return false
        val last = w[w.length - 1]
        val vowel = w[w.length - 2]
        val before = w[w.length - 3]
        if (isVowel(last) || last in "wxy" || !isVowel(vowel) || isVowel(before)) return false
        val (count, stress) = syllables(w)
        return count == 1 || stress == '1'
    }

    private fun syllables(w: String): Pair<Int, Char> {
        val phones = cmudict.pronunciations(w).firstOrNull()
        if (phones != null) return Ipa.lastSyllableStress(phones)
        val groups = Regex("[aeiouy]+").findAll(w.removeSuffix("e")).count().coerceAtLeast(1)
        return groups to '0'
    }

    private fun isVowel(c: Char) = c in "aeiou"
}
