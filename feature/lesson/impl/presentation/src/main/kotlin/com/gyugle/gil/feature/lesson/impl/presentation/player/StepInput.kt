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

import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade

/**
 * The answer in progress on the step on screen, cleared when the next step comes.
 *
 * @property chosen tiles placed so far, as indices into the shuffled bank
 * @property question the passage question being asked
 * @property missedQuestion a passage question so far was answered wrong
 * @property grade the step's grade, once it has one; [feedback] shows it
 */
internal data class StepInput(
    val text: String = "",
    val chosen: List<Int> = emptyList(),
    val picked: Int? = null,
    val question: Int = 0,
    val missedQuestion: Boolean = false,
    val isMeaningShown: Boolean = false,
    val feedback: FeedbackUi? = null,
    val grade: AnswerGrade? = null
)
