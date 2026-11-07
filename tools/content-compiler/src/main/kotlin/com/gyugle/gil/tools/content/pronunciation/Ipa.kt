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
package com.gyugle.gil.tools.content.pronunciation

/**
 * Converts CMUdict ARPAbet to American IPA in the style of learner dictionaries: long vowels carry
 * `ː` (so ship /ʃɪp/ and sheep /ʃiːp/ look different), and stress marks appear only on words of
 * two or more syllables, placed before the stressed syllable.
 */
object Ipa {

    fun fromArpabet(phones: List<String>): String {
        val syllables = syllabify(phones)
        val marked = syllables.size > 1
        return syllables.joinToString("") { syllable ->
            val mark = when {
                !marked -> ""
                syllable.stress == '1' -> "ˈ"
                syllable.stress == '2' -> "ˌ"
                else -> ""
            }
            mark + syllable.phones.joinToString("") { symbol(it) }
        }
    }

    /** Number of syllables and the stress digit of the last one ('0', '1' or '2'). */
    fun lastSyllableStress(phones: List<String>): Pair<Int, Char> {
        val vowels = phones.filter { it.last().isDigit() }
        return vowels.size to (vowels.lastOrNull()?.last() ?: '0')
    }

    private class Syllable(val phones: List<String>, val stress: Char)

    /** Splits phones into syllables, giving each vowel the longest legal onset before it. */
    private fun syllabify(phones: List<String>): List<Syllable> {
        val vowelIndexes = phones.indices.filter { phones[it].last().isDigit() }
        if (vowelIndexes.isEmpty()) return listOf(Syllable(phones, '0'))
        val starts = mutableListOf(0)
        for (k in 1 until vowelIndexes.size) {
            val previousVowel = vowelIndexes[k - 1]
            val vowel = vowelIndexes[k]
            val consonants = phones.subList(previousVowel + 1, vowel)
            var onset = consonants.size
            while (onset > 0 &&
                consonants.subList(consonants.size - onset, consonants.size).joinToString(" ") !in ONSETS
            ) {
                onset--
            }
            starts += vowel - onset
        }
        return starts.mapIndexed { k, start ->
            val end = starts.getOrNull(k + 1) ?: phones.size
            Syllable(phones.subList(start, end), phones[vowelIndexes[k]].last())
        }
    }

    private fun symbol(phone: String): String {
        val base = phone.trimEnd { it.isDigit() }
        val stressed = phone.last() == '1' || phone.last() == '2'
        return when (base) {
            "AH" -> if (stressed) "ʌ" else "ə"

            "ER" -> if (stressed) "ɝː" else "ɚ"

            // Unstressed, these are short, as in happy /ˈhæpi/ and situation /ˌsɪtʃuˈeɪʃən/.
            "IY" -> if (stressed) "iː" else "i"

            "UW" -> if (stressed) "uː" else "u"

            else -> SYMBOLS[base] ?: error("unknown ARPAbet phone $phone")
        }
    }

    private val SYMBOLS = mapOf(
        "AA" to "ɑː", "AE" to "æ", "AO" to "ɔː", "AW" to "aʊ", "AY" to "aɪ", "EH" to "ɛ", "EY" to "eɪ",
        "IH" to "ɪ", "IY" to "iː", "OW" to "oʊ", "OY" to "ɔɪ", "UH" to "ʊ", "UW" to "uː",
        "B" to "b", "CH" to "tʃ", "D" to "d", "DH" to "ð", "F" to "f", "G" to "g", "HH" to "h",
        "JH" to "dʒ", "K" to "k", "L" to "l", "M" to "m", "N" to "n", "NG" to "ŋ", "P" to "p",
        "R" to "r", "S" to "s", "SH" to "ʃ", "T" to "t", "TH" to "θ", "V" to "v", "W" to "w",
        "Y" to "j", "Z" to "z", "ZH" to "ʒ"
    )

    /** Consonant clusters that may start an English syllable. */
    private val ONSETS: Set<String> = buildSet {
        addAll(
            listOf(
                "B", "CH", "D", "DH", "F", "G", "HH", "JH", "K", "L", "M", "N", "P", "R", "S", "SH", "T",
                "TH", "V", "W", "Y", "Z", "ZH"
            )
        )
        addAll(
            listOf(
                "P R", "P L", "B R", "B L", "T R", "D R", "K R", "K L", "G R", "G L", "F R", "F L", "TH R",
                "SH R", "S P", "S T", "S K", "S M", "S N", "S L", "S W", "T W", "D W", "K W", "G W", "TH W",
                "P Y", "B Y", "F Y", "M Y", "K Y", "V Y", "HH Y", "S P R", "S P L", "S T R", "S K R", "S K W",
                "S K Y", "S P Y"
            )
        )
    }
}
