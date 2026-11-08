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
package com.gyugle.gil.feature.curriculum.api

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.Lesson

/**
 * Reads lessons for other features. The content ships with the app and never changes while it
 * runs, so these are one-shot reads.
 */
interface CourseObserver {
    /** [ContentError.NOT_FOUND] when no lesson has [id]; [ContentError.UNAVAILABLE] when reading fails. */
    suspend fun getLesson(id: LessonId): Outcome<Lesson, ContentError>

    /** The lesson the course starts with. [ContentError.UNAVAILABLE] when reading fails. */
    suspend fun getFirstLessonId(): Outcome<LessonId, ContentError>

    /**
     * The first lesson in course order that is not in [completed], or null when every lesson is.
     * [ContentError.UNAVAILABLE] when reading fails.
     */
    suspend fun getNextLesson(completed: Set<LessonId>): Outcome<LessonOverview?, ContentError>

    /** Every stage in course order with its lessons. [ContentError.UNAVAILABLE] when reading fails. */
    suspend fun getStageLessons(): Outcome<List<StageLessons>, ContentError>

    /**
     * Every unit that has lessons, in course order, with its lessons. [ContentError.UNAVAILABLE]
     * when reading fails.
     */
    suspend fun getUnitLessons(): Outcome<List<UnitLessons>, ContentError>
}
