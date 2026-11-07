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

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.review.impl.domain.model.AnswerGrade
import org.junit.jupiter.api.Test

class TypingTest {
    @Test
    fun gradeTyped_caseSpacesAndEndPunctuation_doNotCount() {
        assertThat(gradeTyped("  Thank   YOU. ", "Thank you.")).isEqualTo(AnswerGrade.CORRECT)
        assertThat(gradeTyped("thank you", "Thank you.")).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeTyped_curlyApostrophe_matchesStraightOne() {
        assertThat(gradeTyped("you’re welcome", "You're welcome.")).isEqualTo(AnswerGrade.CORRECT)
    }

    @Test
    fun gradeTyped_oneLetterOffInALongWord_close() {
        assertThat(gradeTyped("thre", "three")).isEqualTo(AnswerGrade.CLOSE)
        assertThat(gradeTyped("thier", "their")).isEqualTo(AnswerGrade.CLOSE)
        assertThat(gradeTyped("tree", "three!")).isEqualTo(AnswerGrade.CLOSE)
    }

    @Test
    fun gradeTyped_oneLetterOffInAShortWord_wrong() {
        assertThat(gradeTyped("is", "it")).isEqualTo(AnswerGrade.WRONG)
        assertThat(gradeTyped("tow", "two")).isEqualTo(AnswerGrade.WRONG)
    }

    @Test
    fun gradeTyped_twoLettersOff_wrong() {
        assertThat(gradeTyped("tre", "three")).isEqualTo(AnswerGrade.WRONG)
    }

    @Test
    fun gradeTyped_blank_wrong() {
        assertThat(gradeTyped("", "seven")).isEqualTo(AnswerGrade.WRONG)
    }
}
