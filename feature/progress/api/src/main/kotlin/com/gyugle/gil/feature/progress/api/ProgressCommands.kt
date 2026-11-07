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

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import kotlin.time.Duration

/** Writes to the learner's record. Each call is stamped with the current time and study day. */
interface ProgressCommands {
    /**
     * Records a finished lesson and starts a review card for each item it introduced. The rating
     * is how the learner did on that item during the lesson. An item that already has a card, from
     * this lesson, another one or a review, keeps its schedule, and its lesson rating is dropped.
     */
    suspend fun completeLesson(
        lessonId: LessonId,
        introduced: Map<StudyItemId, ReviewRating>
    ): EmptyOutcome<DataError.Local>

    /**
     * Reschedules the item and returns its new state. An item without a card gets one.
     *
     * Only for a scheduled review of a card due on or before today. Practice outside the schedule,
     * such as going over past mistakes, must not call this: an early rating moves the schedule.
     */
    suspend fun recordReview(itemId: StudyItemId, rating: ReviewRating): Outcome<CardState, DataError.Local>

    /** Adds [duration] to today's study time. Zero or negative durations change nothing. */
    suspend fun recordStudyTime(duration: Duration): EmptyOutcome<DataError.Local>

    /** Adds the item to the word list or removes it. Repeating the same call changes nothing. */
    suspend fun setSaved(itemId: StudyItemId, saved: Boolean): EmptyOutcome<DataError.Local>

    suspend fun recordMistake(itemId: StudyItemId): EmptyOutcome<DataError.Local>
}
