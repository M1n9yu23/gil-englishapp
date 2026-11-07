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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.gyugle.gil.feature.curriculum.api.Choice
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.PairWord
import com.gyugle.gil.feature.curriculum.api.Sentence
import org.junit.jupiter.api.Test

class StepUiMapperTest {
    private val passage = LessonStep.ReadPassage(sentences = emptyList(), questions = emptyList())

    @Test
    fun order_sameSeed_sameOrderEveryTime() {
        assertThat(StepLayout(passage, seed = 7).order(4)).isEqualTo(StepLayout(passage, seed = 7).order(4))
    }

    @Test
    fun order_passageQuestions_doNotShareOneOrder() {
        // The content lists the right answer first, so one shared order would put it in the same place.
        val differs = (0 until 20).any { seed ->
            val layout = StepLayout(passage, seed)
            layout.order(3, question = 0) != layout.order(3, question = 1)
        }

        assertThat(differs).isTrue()
    }

    @Test
    fun order_soundPair_keepsTheContentOrder() {
        val pair = LessonStep.MinimalPair(listOf(PairWord("ship", "ʃɪp"), PairWord("sheep", "ʃiːp")), 1, null, null)

        assertThat(StepLayout(pair, seed = 3).order(2)).isEqualTo(listOf(0, 1))
    }

    @Test
    fun canCheck_blankWithOptions_isAnsweredByPicking() {
        val blank = LessonStep.FillBlank(
            text = "___ is red.",
            ko = "",
            answers = emptyList(),
            options = listOf(Choice("It", english = true, correct = true, why = null)),
            sentence = Sentence(1, "It is red.", null),
            item = null
        )
        val typed = blank.copy(options = emptyList(), answers = listOf("It"))

        assertThat(blank.canCheck(StepInput(text = "It"))).isFalse()
        assertThat(typed.canCheck(StepInput(text = "It"))).isTrue()
        assertThat(typed.canCheck(StepInput(text = " "))).isFalse()
    }
}
