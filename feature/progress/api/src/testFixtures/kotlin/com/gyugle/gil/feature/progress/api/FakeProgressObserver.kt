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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** A [ProgressObserver] over state flows that a test sets. Everything starts empty. */
class FakeProgressObserver : ProgressObserver {
    val cards = MutableStateFlow<List<CardState>>(emptyList())
    val completedLessons = MutableStateFlow<Set<LessonId>>(emptySet())
    val studyTime = MutableStateFlow<List<DailyStudyTime>>(emptyList())
    val streak = MutableStateFlow(0)
    val savedItems = MutableStateFlow<List<StudyItemId>>(emptyList())
    val mistakes = MutableStateFlow<List<Mistake>>(emptyList())

    /** Gives each of [ids] a card, as if the learner had met them in a lesson on day 0. */
    fun learn(vararg ids: StudyItemId) {
        cards.value += ids.map {
            CardState(
                it,
                stabilityDays = 3.0,
                dueDay = StudyDay(3),
                lastReviewDay = StudyDay(0),
                reviewCount = 1,
                lapseCount = 0
            )
        }
    }

    override fun observeCard(itemId: StudyItemId): Flow<CardState?> =
        cards.map { all -> all.firstOrNull { it.itemId == itemId } }

    override fun observeCards(): Flow<List<CardState>> = cards

    override fun observeDueCards(day: StudyDay): Flow<List<CardState>> =
        cards.map { all -> all.filter { it.dueDay <= day }.sortedBy { it.dueDay } }

    override fun observeCompletedLessons(): Flow<Set<LessonId>> = completedLessons

    override fun observeStudyTime(from: StudyDay, to: StudyDay): Flow<List<DailyStudyTime>> =
        studyTime.map { days -> days.filter { it.day in from..to } }

    override fun observeStreak(today: StudyDay): Flow<Int> = streak

    override fun observeSavedItems(): Flow<List<StudyItemId>> = savedItems

    override fun observeMistakes(): Flow<List<Mistake>> = mistakes
}
