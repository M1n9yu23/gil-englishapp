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
 * A [CourseObserver] over [lessons]. A missing lesson is [ContentError.NOT_FOUND]; [error], when
 * set, is what every call fails with. The first lesson is the first in [lessons], and course order
 * is their order. Every lesson is in one unit titled [unitTitle] unless [units] sets the units,
 * and in [stage] unless [stages] sets the course's stages.
 */
class FakeCourseObserver(vararg lessons: Lesson) : CourseObserver {
    val lessons: MutableList<Lesson> = lessons.toMutableList()
    var error: ContentError? = null
    var unitTitle = LocalizedText("단원", "Unit")
    var stage = StageTag(StageId("stage.s1"), position = 0, title = LocalizedText("단계", "Stage"))
    var stages: List<StageLessons>? = null
    var units: List<UnitLessons>? = null

    override suspend fun getLesson(id: LessonId): Outcome<Lesson, ContentError> {
        error?.let { return Outcome.Failure(it) }
        return lessons.firstOrNull { it.id == id }?.let { Outcome.Success(it) }
            ?: Outcome.Failure(ContentError.NOT_FOUND)
    }

    override suspend fun getFirstLessonId(): Outcome<LessonId, ContentError> {
        error?.let { return Outcome.Failure(it) }
        return lessons.firstOrNull()?.let { Outcome.Success(it.id) } ?: Outcome.Failure(ContentError.UNAVAILABLE)
    }

    override suspend fun getNextLesson(completed: Set<LessonId>): Outcome<LessonOverview?, ContentError> {
        error?.let { return Outcome.Failure(it) }
        val next = lessons.firstOrNull { it.id !in completed }
        return Outcome.Success(
            next?.let { LessonOverview(it.id, it.title, unitTitle, it.steps.size, it.introduced.size) }
        )
    }

    override suspend fun getStageLessons(): Outcome<List<StageLessons>, ContentError> {
        error?.let { return Outcome.Failure(it) }
        return Outcome.Success(stages ?: listOf(StageLessons(stage, lessons.map { it.id })))
    }

    override suspend fun getUnitLessons(): Outcome<List<UnitLessons>, ContentError> {
        error?.let { return Outcome.Failure(it) }
        return Outcome.Success(units ?: listOf(UnitLessons(UnitId("unit.s1-01"), unitTitle, lessons.map { it.id })))
    }
}
