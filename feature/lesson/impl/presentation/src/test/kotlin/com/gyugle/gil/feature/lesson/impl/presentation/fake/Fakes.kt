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
package com.gyugle.gil.feature.lesson.impl.presentation.fake

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import com.gyugle.gil.feature.lesson.impl.domain.repository.LessonRunRepository
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import kotlin.time.Duration
import kotlin.time.Instant

class FakeLessonRunRepository : LessonRunRepository {
    val runs = mutableMapOf<LessonId, LessonRun>()

    override suspend fun get(lessonId: LessonId): LessonRun? = runs[lessonId]

    override suspend fun save(lessonId: LessonId, run: LessonRun): EmptyOutcome<DataError.Local> {
        runs[lessonId] = run
        return Outcome.Success(Unit)
    }

    override suspend fun clear(lessonId: LessonId): EmptyOutcome<DataError.Local> {
        runs.remove(lessonId)
        return Outcome.Success(Unit)
    }
}

/** Records the calls a lesson makes. [completeError], when set, fails [completeLesson]. */
class FakeProgressCommands : ProgressCommands {
    val completed = mutableListOf<Pair<LessonId, Map<StudyItemId, ReviewRating>>>()
    val mistakes = mutableListOf<StudyItemId>()
    val studyTime = mutableListOf<Duration>()
    var completeError: DataError.Local? = null

    override suspend fun completeLesson(
        lessonId: LessonId,
        introduced: Map<StudyItemId, ReviewRating>
    ): EmptyOutcome<DataError.Local> {
        completeError?.let { return Outcome.Failure(it) }
        completed += lessonId to introduced
        return Outcome.Success(Unit)
    }

    override suspend fun recordReview(itemId: StudyItemId, rating: ReviewRating): Outcome<CardState, DataError.Local> =
        error("A lesson never records a review")

    override suspend fun recordStudyTime(duration: Duration): EmptyOutcome<DataError.Local> {
        studyTime += duration
        return Outcome.Success(Unit)
    }

    override suspend fun setSaved(itemId: StudyItemId, saved: Boolean): EmptyOutcome<DataError.Local> =
        Outcome.Success(Unit)

    override suspend fun recordMistake(itemId: StudyItemId): EmptyOutcome<DataError.Local> {
        mistakes += itemId
        return Outcome.Success(Unit)
    }
}

/** A clock that moves only when a test moves it. */
class FakeStudyClock(var now: Instant = Instant.parse("2026-11-07T10:00:00Z")) : StudyClock {
    override fun now(): Instant = now

    override fun dayOf(instant: Instant): StudyDay = StudyDay(instant.epochSeconds / 86_400)
}
