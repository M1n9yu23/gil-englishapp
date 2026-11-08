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
package com.gyugle.gil.feature.stats.impl.presentation.stats

import androidx.compose.runtime.Immutable
import com.gyugle.gil.feature.curriculum.api.LessonId

@Immutable
sealed interface StatsState {
    data object Loading : StatsState

    /** The course or the record could not be read. Trying again may work. */
    data object Failed : StatsState

    /** Nothing studied yet. [firstLesson] is where the course starts, null if it has no lessons. */
    data class Empty(val firstLesson: LessonId?) : StatsState

    data class Ready(val stats: StatsUi) : StatsState
}
