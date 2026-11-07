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
package com.gyugle.gil.feature.curriculum.impl.data.repository

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.api.model.Lesson
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabase
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabaseProvider
import com.gyugle.gil.feature.curriculum.impl.data.dto.StepPayloadDto
import com.gyugle.gil.feature.curriculum.impl.data.dto.StepPayloadJson
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toLesson
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toStage
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toUnitDetail
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toUnitSummary
import com.gyugle.gil.feature.curriculum.impl.domain.model.Stage
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitDetail
import com.gyugle.gil.feature.curriculum.impl.domain.repository.CourseRepository
import javax.inject.Inject
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import timber.log.Timber

class RoomCourseRepository @Inject constructor(private val databases: ContentDatabaseProvider) :
    CourseRepository,
    CourseObserver {

    override suspend fun getStages(): Outcome<List<Stage>, ContentError> = read { database ->
        val dao = database.courseDao()
        val canDos = dao.canDos().groupBy { it.stageId }
        val lessonCounts = dao.lessonCounts()
        val units = dao.units().groupBy { it.stageId }
        val stages = dao.stages().map { stage ->
            stage.toStage(
                canDo = canDos[stage.id].orEmpty(),
                units = units[stage.id].orEmpty()
                    .map { it.toUnitSummary(lessonCount = lessonCounts[it.id] ?: 0) }
            )
        }
        Outcome.Success(stages)
    }

    override suspend fun getUnit(id: UnitId): Outcome<UnitDetail, ContentError> = read { database ->
        val dao = database.courseDao()
        val unit = dao.unit(id.value) ?: return@read Outcome.Failure(ContentError.NOT_FOUND)
        // The compiler checks every unit's stage, so a missing one means a broken file.
        val stage = checkNotNull(dao.stage(unit.stageId)) { "Unit ${unit.id} has no stage ${unit.stageId}" }
        Outcome.Success(unit.toUnitDetail(stage, dao.lessons(unit.id)))
    }

    override suspend fun getLesson(id: LessonId): Outcome<Lesson, ContentError> = read { database ->
        val dao = database.lessonDao()
        val lesson = dao.lesson(id.value) ?: return@read Outcome.Failure(ContentError.NOT_FOUND)
        val items = dao.items(lesson.id)
        // The compiler checks that every introduced item exists, so a missing one means a broken file.
        check(items.size == dao.itemCount(lesson.id)) { "Lesson ${lesson.id} introduces an item that is missing" }
        Outcome.Success(
            lesson.toLesson(
                steps = dao.steps(lesson.id).map { StepPayloadJson.decodeFromString<StepPayloadDto>(it.payload) },
                items = items,
                next = dao.nextLessonId(lesson.coursePosition)
            )
        )
    }

    override suspend fun getFirstLessonId(): Outcome<LessonId, ContentError> = read { database ->
        // The compiler refuses a course without lessons.
        val id = checkNotNull(database.lessonDao().firstLessonId()) { "The course has no lessons" }
        Outcome.Success(LessonId(id))
    }

    /**
     * Any failure to copy, open or read the database is reported as [ContentError.UNAVAILABLE]: the
     * screen offers a retry either way, and the cause goes to the log.
     */
    private suspend fun <T> read(
        block: suspend (ContentDatabase) -> Outcome<T, ContentError>
    ): Outcome<T, ContentError> = try {
        block(databases.get())
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Timber.e(e, "Could not read the content database")
        Outcome.Failure(ContentError.UNAVAILABLE)
    }
}
