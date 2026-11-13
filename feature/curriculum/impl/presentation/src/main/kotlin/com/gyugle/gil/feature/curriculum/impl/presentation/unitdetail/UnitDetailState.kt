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
package com.gyugle.gil.feature.curriculum.impl.presentation.unitdetail

import androidx.compose.runtime.Immutable
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.impl.presentation.ContentErrorUi
import com.gyugle.gil.feature.curriculum.impl.presentation.LocalizedTextUi

/** [skipAhead] asks before a lesson that comes after one not done yet. */
@Immutable
data class UnitDetailState(
    val isLoading: Boolean = true,
    val unit: UnitDetailUi? = null,
    val error: ContentErrorUi? = null,
    val skipAhead: SkipAheadUi? = null
)

/** The learner picked [picked], but [earlier], the course's next lesson, is not done yet. */
@Immutable
data class SkipAheadUi(val picked: LessonId, val earlier: LessonId, val earlierTitle: LocalizedTextUi)
