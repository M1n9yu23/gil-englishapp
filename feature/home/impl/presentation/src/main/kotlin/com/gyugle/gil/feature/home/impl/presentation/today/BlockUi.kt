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
package com.gyugle.gil.feature.home.impl.presentation.today

import androidx.compose.runtime.Immutable
import com.gyugle.gil.feature.curriculum.api.LessonId

@Immutable
sealed interface BlockUi {
    /** [count] cards due, about [minutes] long. [more] is the rest after the lesson. */
    data class Review(val count: Int, val minutes: Int, val more: Boolean) : BlockUi

    data object ReviewDone : BlockUi

    /** [enoughNewWords] when the lesson would bring more new words than today has room for. */
    data class Lesson(
        val id: LessonId,
        val title: LocalizedTextUi,
        val unitTitle: LocalizedTextUi,
        val minutes: Int,
        val enoughNewWords: Boolean
    ) : BlockUi

    data object CourseDone : BlockUi

    /** Practice: listening mode, for what the learner has met. */
    data object Listening : BlockUi
}
