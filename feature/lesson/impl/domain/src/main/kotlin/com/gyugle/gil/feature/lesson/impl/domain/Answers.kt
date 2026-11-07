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

import com.gyugle.gil.feature.curriculum.api.model.LessonStep
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.DictationCheck
import com.gyugle.gil.feature.lesson.impl.domain.model.MarkedWord

/** A spelled word, compared without case. One letter off counts as [AnswerGrade.CLOSE]. */
fun gradeSpelling(typed: String, answer: String): AnswerGrade =
    gradeWord(typed.normalizeTyped().trimPunctuation(), listOf(answer), caseSensitiveSingleLetter = false)

/**
 * A typed blank. Compared without case, except a one-letter answer: a blank that asks for the
 * lowercase q of `P, p, Q, ___.` is wrong as Q.
 */
fun gradeFillBlank(typed: String, answers: List<String>): AnswerGrade =
    gradeWord(typed.normalizeTyped().trimPunctuation(), answers, caseSensitiveSingleLetter = true)

/** Tiles in [chosen] order, right when they make the answer or one of its other accepted orders. */
fun gradeArrange(chosen: List<String>, step: LessonStep.Arrange): AnswerGrade =
    if (chosen == step.tiles || chosen in step.accepted) AnswerGrade.CORRECT else AnswerGrade.WRONG

/**
 * A typed sentence, word by word. Case and punctuation do not count. A sentence of letter names
 * (`B, E, D.`) is also right written as one word (bed). One letter off in one long word is
 * [AnswerGrade.CLOSE].
 */
fun checkDictation(typed: String, sentence: String): DictationCheck {
    val expected = tokenize(sentence)
    val written = tokenize(typed)
    val matched = commonSubsequenceMask(written.map { it.key }, expected.map { it.key })
    val marks = written.mapIndexed { index, token -> MarkedWord(token.text, matched[index]) }
    val grade = when {
        written.map { it.key } == expected.map { it.key } -> AnswerGrade.CORRECT

        expected.all { it.key.length == 1 } &&
            written.joinToString("") { it.key } == expected.joinToString("") { it.key } ->
            AnswerGrade.CORRECT

        isOneSlip(written.map { it.key }, expected.map { it.key }) -> AnswerGrade.CLOSE

        else -> AnswerGrade.WRONG
    }
    // Letter names typed as one word are right, so the word is marked right too.
    return DictationCheck(grade, if (grade == AnswerGrade.CORRECT) marks.map { it.copy(matched = true) } else marks)
}

private fun isOneSlip(written: List<String>, expected: List<String>): Boolean {
    if (written.size != expected.size) return false
    val different = written.indices.filter { written[it] != expected[it] }
    val index = different.singleOrNull() ?: return false
    return expected[index].length >= MIN_LENGTH_FOR_CLOSE && editDistance(written[index], expected[index]) == 1
}

private fun gradeWord(typed: String, answers: List<String>, caseSensitiveSingleLetter: Boolean): AnswerGrade {
    val normalized = answers.map { it.normalizeTyped() }
    fun same(answer: String) = if (caseSensitiveSingleLetter && answer.codePointCount(0, answer.length) == 1) {
        typed == answer
    } else {
        typed.equals(answer, ignoreCase = true)
    }
    return when {
        normalized.any(::same) -> AnswerGrade.CORRECT

        normalized.any { it.length >= MIN_LENGTH_FOR_CLOSE && editDistance(typed.lowercase(), it.lowercase()) == 1 } ->
            AnswerGrade.CLOSE

        else -> AnswerGrade.WRONG
    }
}

/** A full stop or comma typed after a word is not part of the answer. */
private fun String.trimPunctuation(): String = trim { it in ".,!?;:\"" || it.isWhitespace() }
