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
package com.gyugle.gil.feature.review.impl.presentation.session

import androidx.compose.runtime.Immutable

/**
 * The end of a session. [again] are the items answered wrong. [remaining] counts the cards still
 * due today; when there are none, [nextInDays] says when the next review is, null when the learner
 * has no cards at all.
 */
@Immutable
data class ResultUi(
    val answered: Int,
    val right: Int,
    val again: List<ResultItemUi>,
    val remaining: Int,
    val nextInDays: Int?
)
