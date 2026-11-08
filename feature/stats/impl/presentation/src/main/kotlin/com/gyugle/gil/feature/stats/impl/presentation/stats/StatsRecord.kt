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

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.DailyStudyTime

/** Everything the Stats tab reads, for one study day. [studyTime] is in date order. */
internal class StatsRecord(
    val studyTime: List<DailyStudyTime>,
    val cards: List<CardState>,
    val completed: Set<LessonId>,
    val streak: Int
) {
    /**
     * Nothing finished and nothing learned yet. Time spent in a lesson left halfway does not count:
     * the empty state says the record starts with the first finished lesson.
     */
    val isEmpty: Boolean get() = cards.isEmpty() && completed.isEmpty()
}
