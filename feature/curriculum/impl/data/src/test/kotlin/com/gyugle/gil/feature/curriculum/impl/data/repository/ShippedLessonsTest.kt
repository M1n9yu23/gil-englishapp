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

import android.app.Application
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.model.Lesson
import com.gyugle.gil.feature.curriculum.api.model.LessonStep
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabaseHolder
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Reads every lesson of the content that ships, so a payload the app cannot decode fails here. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ShippedLessonsTest {
    private val context = RuntimeEnvironment.getApplication()
    private val repository = RoomCourseRepository(ContentDatabaseHolder(context))

    private fun manifestLessonCount(): Int = Json.parseToJsonElement(
        context.assets.open("content/manifest.json").bufferedReader().use { it.readText() }
    ).jsonObject.getValue("counts").jsonObject.getValue("lessons").jsonPrimitive.int

    /** Every lesson in course order, found through the stages and units like the path map does. */
    private suspend fun allLessons(): List<Lesson> {
        val stages = (repository.getStages() as Outcome.Success).data
        val ids = stages.flatMap { it.units }.flatMap { unit ->
            (repository.getUnit(unit.id) as Outcome.Success).data.lessons.map { it.id }
        }
        return ids.map { (repository.getLesson(it) as Outcome.Success).data }
    }

    @Test
    fun getLesson_everyShippedLesson_decodesEveryStep() = runTest {
        val lessons = allLessons()

        assertThat(lessons.size).isEqualTo(manifestLessonCount())
        lessons.forEach { assertThat(it.steps, name = it.id.value).isNotEmpty() }
        // Every kind of step occurs, so each decoder ran on real content at least once.
        assertThat(lessons.flatMap { it.steps }.map { it::class }.toSet()).containsExactlyInAnyOrder(
            LessonStep.Explain::class,
            LessonStep.Listen::class,
            LessonStep.Shadow::class,
            LessonStep.ChooseMeaning::class,
            LessonStep.ListenChoose::class,
            LessonStep.ChooseEnglish::class,
            LessonStep.Arrange::class,
            LessonStep.FillBlank::class,
            LessonStep.Spell::class,
            LessonStep.Dictation::class,
            LessonStep.MinimalPair::class,
            LessonStep.ReadPassage::class,
            LessonStep.Dialogue::class
        )
    }

    @Test
    fun getLesson_everyShippedLesson_linksToTheNextInCourseOrder() = runTest {
        val lessons = allLessons()

        lessons.zipWithNext().forEach { (lesson, next) ->
            assertThat(lesson.next, name = lesson.id.value).isEqualTo(next.id)
        }
        assertThat(lessons.last().next).isNull()
        assertThat(repository.getFirstLessonId()).isEqualTo(Outcome.Success(lessons.first().id))
    }
}
