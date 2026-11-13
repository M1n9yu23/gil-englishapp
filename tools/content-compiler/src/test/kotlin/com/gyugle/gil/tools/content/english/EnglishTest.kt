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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.prop
import com.gyugle.gil.tools.content.SampleContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class EnglishTest {

    private val inflector = Inflector(SampleContent.cmudict)

    @ParameterizedTest
    @CsvSource(
        "stop, PAST, stopped",
        "visit, PAST, visited",
        "begin, ING, beginning",
        "visit, ING, visiting",
        "carry, PAST, carried",
        "play, PAST, played",
        "make, ING, making",
        "see, ING, seeing",
        "lie, ING, lying",
        "watch, THIRD, watches",
        "go, THIRD, goes",
        "study, THIRD, studies",
        "photo, PLURAL, photos",
        "box, PLURAL, boxes",
        "big, COMPARATIVE, bigger",
        "happy, SUPERLATIVE, happiest",
        "nice, COMPARATIVE, nicer"
    )
    fun regular_regularWord_givesRegularForm(word: String, kind: FormKind, expected: String) {
        assertThat(inflector.regular(word, kind)).isEqualTo(expected)
    }

    @Test
    fun regular_longAdjective_hasNoComparative() {
        assertThat(inflector.regular("beautiful", FormKind.COMPARATIVE)).isNull()
    }

    @Test
    fun regular_phrase_hasNoForms() {
        assertThat(inflector.regular("next to", FormKind.PLURAL)).isNull()
    }

    @Test
    fun tokenize_contractionsHyphensAndNumbers_stayOneToken() {
        val tokens = Tokenizer.tokenize("I don’t know twenty-one people, 1,000 or 3.5.")

        assertThat(
            tokens.map {
                it.text
            }
        ).containsExactly("I", "don’t", "know", "twenty-one", "people", "1,000", "or", "3.5")
        assertThat(tokens[1].key).isEqualTo("don't")
    }

    @Test
    fun apostrophes_onlyBetweenLetters_becomeTypographic() {
        assertThat(apostrophes("What's your name? 'Hi' 인사")).isEqualTo("What’s your name? 'Hi' 인사")
    }

    @Test
    fun parse_allMarks_removedWithOffsetsKept() {
        val result = SentenceMarkup.parse("[@Mina]{who} [{runs|sense.run.v.manage}]{do} [a cafe]{what}.")

        assertThat(result).isInstanceOf<MarkupResult.Parsed>()
        val sentence = (result as MarkupResult.Parsed).sentence
        assertThat(sentence.text).isEqualTo("Mina runs a cafe.")
        assertThat(sentence.senseMarks).containsExactly(SenseMark(5, 9, "sense.run.v.manage"))
        assertThat(sentence.chunks.map { it.role }).containsExactly(ChunkRole.WHO, ChunkRole.DO, ChunkRole.WHAT)
        assertThat(sentence.properNouns).containsExactly(0..3)
    }

    @Test
    fun parse_lowercaseProperNoun_isInvalid() {
        assertThat(SentenceMarkup.parse("Hi, @mina.")).isInstanceOf<MarkupResult.Invalid>()
    }

    @Test
    fun parse_senseMarkWithoutSense_isInvalid() {
        assertThat(SentenceMarkup.parse("I {run} fast.")).isInstanceOf<MarkupResult.Invalid>()
    }

    @Test
    fun split_contraction_givesBaseAndAlternatives() {
        assertThat(Contractions.split("can't")).isEqualTo(Contraction("can", listOf("not"), canBePossessive = false))
        assertThat(
            Contractions.split("i'd")
        ).isEqualTo(Contraction("i", listOf("would", "had"), canBePossessive = false))
        assertThat(Contractions.split("mina's")).isNotNull().prop(Contraction::canBePossessive).isEqualTo(true)
        assertThat(Contractions.split("student")).isNull()
    }
}
