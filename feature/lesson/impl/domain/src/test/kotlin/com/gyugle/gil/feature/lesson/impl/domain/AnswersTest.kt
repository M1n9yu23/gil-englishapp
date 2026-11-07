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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.model.LessonStep
import com.gyugle.gil.feature.curriculum.api.model.Sentence
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.DictationCheck
import com.gyugle.gil.feature.lesson.impl.domain.model.MarkedWord
import org.junit.jupiter.api.Test

class AnswersTest {

    @Test
    fun gradeSpelling_sameWordOtherCaseAndSpaces_correct() {
        assertThat(gradeSpelling("  Apple ", "apple")).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeSpelling_fullStopAfterTheWord_correct() {
        assertThat(gradeSpelling("apple.", "apple")).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeSpelling_oneLetterMissing_close() {
        assertThat(gradeSpelling("aple", "apple")).isEqualTo(AnswerGrade.CLOSE)
    }

    @Test
    fun gradeSpelling_twoLettersSwapped_close() {
        assertThat(gradeSpelling("appel", "apple")).isEqualTo(AnswerGrade.CLOSE)
    }

    @Test
    fun gradeSpelling_twoLettersOff_wrong() {
        assertThat(gradeSpelling("abble", "apple")).isEqualTo(AnswerGrade.WRONG)
        assertThat(gradeSpelling("apl", "apple")).isEqualTo(AnswerGrade.WRONG)
    }

    @Test
    fun gradeSpelling_oneLetterOffInAShortWord_wrong() {
        // "bad" is a word of its own, not a slip of "bed".
        assertThat(gradeSpelling("bad", "bed")).isEqualTo(AnswerGrade.WRONG)
    }

    @Test
    fun gradeSpelling_oneLetterOffInAFourLetterWord_close() {
        assertThat(gradeSpelling("deks", "desk")).isEqualTo(AnswerGrade.CLOSE)
    }

    @Test
    fun gradeSpelling_empty_wrong() {
        assertThat(gradeSpelling("", "apple")).isEqualTo(AnswerGrade.WRONG)
    }

    @Test
    fun gradeFillBlank_anyListedAnswer_correct() {
        assertThat(gradeFillBlank("Hi", listOf("Hello", "Hi"))).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeFillBlank_typewriterApostrophe_matchesTypographicAnswer() {
        assertThat(gradeFillBlank("I'm", listOf("I’m"))).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeFillBlank_singleLetterOtherCase_wrong() {
        assertThat(gradeFillBlank("Q", listOf("q"))).isEqualTo(AnswerGrade.WRONG)
        assertThat(gradeFillBlank("q", listOf("q"))).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeFillBlank_longerWordOtherCase_correct() {
        assertThat(gradeFillBlank("my", listOf("My"))).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeArrange_answerOrderOrAcceptedOrder_correct() {
        val step = arrange(tiles = listOf("Today", "I", "run"), accepted = listOf(listOf("I", "run", "today")))

        assertThat(gradeArrange(listOf("Today", "I", "run"), step)).isEqualTo(AnswerGrade.CORRECT)
        assertThat(gradeArrange(listOf("I", "run", "today"), step)).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeArrange_otherOrderOrMissingTile_wrong() {
        val step = arrange(tiles = listOf("I", "run"), accepted = emptyList())

        assertThat(gradeArrange(listOf("run", "I"), step)).isEqualTo(AnswerGrade.WRONG)
        assertThat(gradeArrange(listOf("I"), step)).isEqualTo(AnswerGrade.WRONG)
        assertThat(gradeArrange(listOf("I", "run", "fast"), step)).isEqualTo(AnswerGrade.WRONG)
    }

    @Test
    fun checkDictation_caseAndPunctuationDiffer_correct() {
        assertThat(checkDictation("light right", "Light, right.")).isEqualTo(
            DictationCheck(AnswerGrade.CORRECT, listOf(MarkedWord("light", true), MarkedWord("right", true)))
        )
    }

    @Test
    fun checkDictation_contractionWithTypewriterApostrophe_correct() {
        assertThat(checkDictation("i'm fine", "I’m fine.").grade).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun checkDictation_letterNamesWrittenAsOneWord_correct() {
        assertThat(checkDictation("bed", "B, E, D.")).isEqualTo(
            DictationCheck(AnswerGrade.CORRECT, listOf(MarkedWord("bed", true)))
        )
        assertThat(checkDictation("B E D", "B, E, D.").grade).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun checkDictation_letterNamesInTheWrongOrder_wrong() {
        assertThat(checkDictation("bde", "B, E, D.").grade).isEqualTo(AnswerGrade.WRONG)
    }

    @Test
    fun checkDictation_oneSlipInALongWord_close() {
        assertThat(checkDictation("Good mornig", "Good morning.").grade).isEqualTo(AnswerGrade.CLOSE)
    }

    @Test
    fun checkDictation_wrongWord_marksOnlyThatWord() {
        assertThat(checkDictation("Red lip", "Red leg.")).isEqualTo(
            DictationCheck(AnswerGrade.WRONG, listOf(MarkedWord("Red", true), MarkedWord("lip", false)))
        )
    }

    @Test
    fun checkDictation_missingWord_wrongAndKeepsTheRestMatched() {
        assertThat(checkDictation("Not", "Not now.")).isEqualTo(
            DictationCheck(AnswerGrade.WRONG, listOf(MarkedWord("Not", true)))
        )
    }

    @Test
    fun checkDictation_empty_wrong() {
        assertThat(checkDictation("  ", "Ten.")).isEqualTo(DictationCheck(AnswerGrade.WRONG, emptyList()))
    }

    private fun arrange(tiles: List<String>, accepted: List<List<String>>) = LessonStep.Arrange(
        ko = "",
        answer = Sentence(1, tiles.joinToString(" "), null),
        tiles = tiles,
        accepted = accepted,
        extra = emptyList(),
        item = null
    )
}
