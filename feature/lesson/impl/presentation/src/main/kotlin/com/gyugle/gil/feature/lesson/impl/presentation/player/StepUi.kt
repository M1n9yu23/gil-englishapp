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

import androidx.compose.runtime.Immutable
/** The step on screen, in the form its composable draws. */
@Immutable
sealed interface StepUi {
    data class Explain(val paragraphs: List<String>, val examples: List<SentenceUi>) : StepUi

    data class Listen(val sentence: SentenceUi, val isMeaningShown: Boolean) : StepUi

    data class Shadow(val sentence: SentenceUi, val recorder: RecorderUi) : StepUi

    /**
     * Picking one of [options]. [correct] and [picked] are known once the step is answered; before
     * that [correct] is null, so nothing on screen gives the answer away.
     */
    data class Choice(val prompt: ChoicePrompt, val options: List<OptionUi>, val picked: Int?, val correct: Int?) :
        StepUi

    /** [bank] is every tile in shuffled order; [chosen] holds indices into it, in sentence order. */
    data class Arrange(val ko: String, val bank: List<TileUi>, val chosen: List<Int>) : StepUi

    data class Typed(val prompt: TypedPrompt, val text: String) : StepUi
}
