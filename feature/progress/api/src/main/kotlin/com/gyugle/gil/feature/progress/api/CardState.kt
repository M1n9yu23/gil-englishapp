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
package com.gyugle.gil.feature.progress.api

import com.gyugle.gil.feature.curriculum.api.StudyItemId

/**
 * The review schedule of one item the learner has met.
 *
 * @property stabilityDays how many days after the last review the chance of recalling the item
 *   falls to 90%. It grows with each successful review, so it also tells how well the item is known.
 * @property reviewCount every rating so far, including the first one in the lesson that introduced it.
 * @property lapseCount how often the item was forgotten after that first rating.
 */
data class CardState(
    val itemId: StudyItemId,
    val stabilityDays: Double,
    val dueDay: StudyDay,
    val lastReviewDay: StudyDay,
    val reviewCount: Int,
    val lapseCount: Int
)
