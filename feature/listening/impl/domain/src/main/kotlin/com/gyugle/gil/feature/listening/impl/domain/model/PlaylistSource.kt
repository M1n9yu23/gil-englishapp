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
package com.gyugle.gil.feature.listening.impl.domain.model

import com.gyugle.gil.feature.curriculum.api.UnitId

/** What a listening session plays. Every source holds only what the learner has already met. */
sealed interface PlaylistSource {
    /** Items due for review today. Hearing them is not their review. */
    data object DueToday : PlaylistSource

    /** The items of the last lessons finished, in course order. */
    data object Recent : PlaylistSource

    /** The items of the finished lessons of one unit. */
    data class CourseUnit(val id: UnitId) : PlaylistSource

    /** The dialogues of finished lessons. */
    data object Dialogues : PlaylistSource
}
