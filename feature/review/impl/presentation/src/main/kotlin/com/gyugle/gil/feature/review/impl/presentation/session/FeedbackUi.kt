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
 * The verdict on an answer. [answer] is the right English when a typed answer was not exact;
 * [note] is Korean that teaches something more, such as the meaning of a word only heard.
 */
@Immutable
data class FeedbackUi(val grade: GradeUi, val answer: String? = null, val note: String? = null)
