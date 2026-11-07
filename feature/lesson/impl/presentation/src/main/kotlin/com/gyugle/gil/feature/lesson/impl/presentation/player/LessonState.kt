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
import com.gyugle.gil.core.presentation.UiText

@Immutable
data class LessonState(
    val title: LocalizedTextUi? = null,
    val phase: LessonPhase = LessonPhase.Loading,
    val isExitConfirmShown: Boolean = false
)

@Immutable
sealed interface LessonPhase {
    data object Loading : LessonPhase

    data class Failed(val error: LessonErrorUi) : LessonPhase

    /** A run was left halfway at [step] of [count]; the learner picks continuing or starting over. */
    data class AskResume(val step: Int, val count: Int) : LessonPhase

    /**
     * One step on screen. [stepNumber] counts from 1 over [stepCount], which grows when a missed
     * step is queued again. [isAgain] marks a step playing for the second time.
     *
     * @property hasVoice whether sound buttons can play; without a voice they are hidden
     * @property showsNoVoiceNotice the one-time note that listening steps fall back to text
     * @property canCheck whether the answer so far can be checked
     */
    data class Playing(
        val stepNumber: Int,
        val stepCount: Int,
        val isAgain: Boolean,
        val hasVoice: Boolean,
        val showsNoVoiceNotice: Boolean,
        val step: StepUi,
        val feedback: FeedbackUi?,
        val canCheck: Boolean
    ) : LessonPhase

    data class Finished(val result: ResultUi) : LessonPhase
}

@Immutable
data class LessonErrorUi(val title: UiText, val body: UiText, val canRetry: Boolean)
