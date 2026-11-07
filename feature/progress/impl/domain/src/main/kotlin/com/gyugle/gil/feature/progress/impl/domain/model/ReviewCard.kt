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
package com.gyugle.gil.feature.progress.impl.domain.model

import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.StudyDay

/** The stored schedule of one item: the public card plus the difficulty FSRS needs. */
data class ReviewCard(
    val itemId: StudyItemId,
    val memory: MemoryState,
    val dueDay: StudyDay,
    val lastReviewDay: StudyDay,
    val reviewCount: Int,
    val lapseCount: Int
)
