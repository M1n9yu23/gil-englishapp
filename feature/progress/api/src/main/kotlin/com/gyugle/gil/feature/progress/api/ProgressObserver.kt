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

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import kotlinx.coroutines.flow.Flow

/**
 * Reads the learner's record. Every flow emits again when the record changes.
 *
 * A flow that takes a day keeps that day: to follow the date past 04:00, collect again with the
 * new [StudyClock.today].
 */
interface ProgressObserver {
    /** Null until the item has a card, started by a finished lesson or a first review. */
    fun observeCard(itemId: StudyItemId): Flow<CardState?>

    /** One card for every item the learner has met, which is the list of learned items. */
    fun observeCards(): Flow<List<CardState>>

    /** Cards due on or before [day], the longest overdue first. */
    fun observeDueCards(day: StudyDay): Flow<List<CardState>>

    fun observeCompletedLessons(): Flow<Set<LessonId>>

    /** Days from [from] to [to], both included, that have any study time, in date order. */
    fun observeStudyTime(from: StudyDay, to: StudyDay): Flow<List<DailyStudyTime>>

    /**
     * The number of study days in the current streak. A study day has at least five minutes of
     * study. The streak survives up to two days off in any seven days and ends on the third.
     * [today] does not count as a day off before it is over.
     */
    fun observeStreak(today: StudyDay): Flow<Int>

    /** The learner's word list, the most recently saved first. */
    fun observeSavedItems(): Flow<List<StudyItemId>>

    /** The most recent mistake first. */
    fun observeMistakes(): Flow<List<Mistake>>
}
