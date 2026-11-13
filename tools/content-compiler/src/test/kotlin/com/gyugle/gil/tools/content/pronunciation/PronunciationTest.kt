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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.gyugle.gil.tools.content.SampleContent
import com.gyugle.gil.tools.content.font.FontCoverage
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class PronunciationTest {

    @ParameterizedTest
    @CsvSource(
        "ship, ʃɪp",
        "sheep, ʃiːp",
        "run, rʌn",
        "student, ˈstuːdənt",
        "happy, ˈhæpi",
        "begin, bɪˈgɪn",
        "morning, ˈmɔːrnɪŋ",
        "teacher, ˈtiːtʃɚ",
        "information, ˌɪnfɚˈmeɪʃən",
        "the, ðə"
    )
    fun fromArpabet_firstPronunciation_givesLearnerDictionaryIpa(word: String, expected: String) {
        val phones = SampleContent.cmudict.pronunciations(word).first()

        assertThat(Ipa.fromArpabet(phones)).isEqualTo(expected)
    }

    @Test
    fun parse_variantsAndComments_groupedUnderTheWord() {
        val dict = Cmudict.parse(sequenceOf("read R EH1 D", "read(2) R IY1 D # verb", "", "Ship SH IH1 P"))

        assertThat(dict.pronunciations("read")).isEqualTo(listOf(listOf("R", "EH1", "D"), listOf("R", "IY1", "D")))
        assertThat(dict.pronunciations("SHIP").size).isEqualTo(1)
    }

    @Test
    fun of_charis_drawsIpaButNotHangul() {
        val charis = FontCoverage.of(SampleContent.fontFiles("charis"))

        "ʃɪŋəɚˈˌː".codePoints().forEach { assertThat(charis.covers(it)).isTrue() }
        assertThat(charis.covers('가'.code)).isFalse()
    }

    @Test
    fun of_uiFont_drawsLatinAndHangulButNotEmoji() {
        val ui = FontCoverage.of(SampleContent.fontFiles("pretendard"))

        assertThat(ui.covers('A'.code)).isTrue()
        assertThat((0xAC00..0xD7A3).all(ui::covers)).isTrue()
        assertThat(ui.covers(0x1F600)).isFalse()
    }
}
