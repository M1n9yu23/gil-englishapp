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

/**
 * Records the calls made to it. [completeError], [saveError], [reviewError] and [studyTimeError],
 * when set, fail [completeLesson] and [skipLessons], [setSaved], [recordReview], and
 * [recordStudyTime]. With an [observer], [setSaved] also changes its
 * word list, newest first, [recordReview] moves the item's card to [nextDueDay], and
 * [clearMistake] takes the item off its mistakes.
 */
class FakeProgressCommands(private val observer: FakeProgressObserver? = null) : ProgressCommands {
    val completed = mutableListOf<Pair<LessonId, Map<StudyItemId, ReviewRating>>>()
    val skipped = mutableListOf<Pair<List<LessonId>, Map<StudyItemId, ReviewRating>>>()
    val mistakes = mutableListOf<StudyItemId>()
    val studyTime = mutableListOf<Duration>()
    val saved = mutableListOf<Pair<StudyItemId, Boolean>>()
    val reviews = mutableListOf<Pair<StudyItemId, ReviewRating>>()
    val clearedMistakes = mutableListOf<StudyItemId>()
    var completeError: DataError.Local? = null
    var saveError: DataError.Local? = null
    var reviewError: DataError.Local? = null
    var studyTimeError: DataError.Local? = null
    var nextDueDay = StudyDay(100)

    override suspend fun completeLesson(
        lessonId: LessonId,
        introduced: Map<StudyItemId, ReviewRating>
    ): EmptyOutcome<DataError.Local> {
        completeError?.let { return Outcome.Failure(it) }
        completed += lessonId to introduced
        return Outcome.Success(Unit)
    }

    override suspend fun skipLessons(
        lessonIds: List<LessonId>,
        tested: Map<StudyItemId, ReviewRating>
    ): EmptyOutcome<DataError.Local> {
        completeError?.let { return Outcome.Failure(it) }
        skipped += lessonIds to tested
        return Outcome.Success(Unit)
    }

    override suspend fun recordReview(itemId: StudyItemId, rating: ReviewRating): Outcome<CardState, DataError.Local> {
        reviewError?.let { return Outcome.Failure(it) }
        reviews += itemId to rating
        val before = observer?.cards?.value?.firstOrNull { it.itemId == itemId }
        val after = CardState(
            itemId = itemId,
            stabilityDays = before?.stabilityDays ?: 1.0,
            dueDay = nextDueDay,
            lastReviewDay = before?.lastReviewDay ?: StudyDay(0),
            reviewCount = (before?.reviewCount ?: 0) + 1,
            lapseCount = (before?.lapseCount ?: 0) + if (rating == ReviewRating.AGAIN) 1 else 0
        )
        observer?.cards?.let { cards -> cards.value = cards.value.filter { it.itemId != itemId } + after }
        return Outcome.Success(after)
    }

    override suspend fun recordStudyTime(duration: Duration): EmptyOutcome<DataError.Local> {
        studyTimeError?.let { return Outcome.Failure(it) }
        studyTime += duration
        return Outcome.Success(Unit)
    }

    override suspend fun setSaved(itemId: StudyItemId, saved: Boolean): EmptyOutcome<DataError.Local> {
        saveError?.let { return Outcome.Failure(it) }
        this.saved += itemId to saved
        observer?.savedItems?.value?.let { current ->
            observer.savedItems.value = if (saved) listOf(itemId) + (current - itemId) else current - itemId
        }
        return Outcome.Success(Unit)
    }

    override suspend fun recordMistake(itemId: StudyItemId): EmptyOutcome<DataError.Local> {
        mistakes += itemId
        return Outcome.Success(Unit)
    }

    override suspend fun clearMistake(itemId: StudyItemId): EmptyOutcome<DataError.Local> {
        clearedMistakes += itemId
        observer?.mistakes?.let { list -> list.value = list.value.filter { it.itemId != itemId } }
        return Outcome.Success(Unit)
    }
}
