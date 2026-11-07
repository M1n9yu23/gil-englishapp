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
import androidx.room.Room
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabase
import com.gyugle.gil.feature.curriculum.impl.domain.error.ContentError
import com.gyugle.gil.feature.curriculum.impl.domain.model.LessonSummary
import com.gyugle.gil.feature.curriculum.impl.domain.model.LocalizedText
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitDetail
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitSummary
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import timber.log.Timber

/**
 * Runs the real queries on an in-memory copy of the schema. Rows go in out of order to check that
 * the course order comes back.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomCourseRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        ContentDatabase::class.java
    )
        .build()
        .apply {
            openHelper.writableDatabase.apply {
                execSQL("INSERT INTO stage (id, position, titleKo, titleEn) VALUES ('stage.two', 1, '둘', 'Two')")
                execSQL("INSERT INTO stage (id, position, titleKo, titleEn) VALUES ('stage.one', 0, '하나', 'One')")
                execSQL(
                    "INSERT INTO stage_can_do (stageId, position, textKo, textEn) VALUES ('stage.one', 1, '두째', 'Second')"
                )
                execSQL(
                    "INSERT INTO stage_can_do (stageId, position, textKo, textEn) VALUES ('stage.one', 0, '첫째', 'First')"
                )
                execSQL(
                    "INSERT INTO unit (id, stageId, position, titleKo, titleEn) VALUES ('unit.b', 'stage.one', 1, '나', 'B')"
                )
                execSQL(
                    "INSERT INTO unit (id, stageId, position, titleKo, titleEn) VALUES ('unit.a', 'stage.one', 0, '가', 'A')"
                )
                lesson(id = "lesson.a.2", position = 1, coursePosition = 2)
                lesson(id = "lesson.a.1", position = 0, coursePosition = 1)
            }
        }
    private val repository = RoomCourseRepository { database }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.lesson(id: String, position: Int, coursePosition: Int) =
        execSQL(
            "INSERT INTO lesson (id, unitId, position, coursePosition, titleKo, titleEn, goalKo, goalEn) " +
                "VALUES ('$id', 'unit.a', $position, $coursePosition, '$id ko', '$id en', '$id 목표', '$id goal')"
        )

    @After
    fun close() = database.close()

    @Test
    fun getStages_rowsOutOfOrder_returnsCourseOrderWithLessonCounts() = runTest {
        val stages = (repository.getStages() as Outcome.Success).data

        assertThat(stages.map { it.id }).isEqualTo(listOf(StageId("stage.one"), StageId("stage.two")))
        assertThat(stages[0].canDo).isEqualTo(listOf(LocalizedText("첫째", "First"), LocalizedText("두째", "Second")))
        assertThat(stages[0].units).isEqualTo(
            listOf(
                UnitSummary(UnitId("unit.a"), LocalizedText("가", "A"), lessonCount = 2),
                UnitSummary(UnitId("unit.b"), LocalizedText("나", "B"), lessonCount = 0)
            )
        )
        assertThat(stages[1].canDo).isEqualTo(emptyList())
        assertThat(stages[1].units).isEqualTo(emptyList())
    }

    @Test
    fun getUnit_knownId_returnsStageTitleAndLessonsInOrder() = runTest {
        assertThat(repository.getUnit(UnitId("unit.a"))).isEqualTo(
            Outcome.Success(
                UnitDetail(
                    id = UnitId("unit.a"),
                    stageTitle = LocalizedText("하나", "One"),
                    title = LocalizedText("가", "A"),
                    lessons = listOf(
                        LessonSummary(
                            LessonId("lesson.a.1"),
                            LocalizedText("lesson.a.1 ko", "lesson.a.1 en"),
                            LocalizedText("lesson.a.1 목표", "lesson.a.1 goal")
                        ),
                        LessonSummary(
                            LessonId("lesson.a.2"),
                            LocalizedText("lesson.a.2 ko", "lesson.a.2 en"),
                            LocalizedText("lesson.a.2 목표", "lesson.a.2 goal")
                        )
                    )
                )
            )
        )
    }

    @Test
    fun getUnit_unknownId_notFound() = runTest {
        assertThat(repository.getUnit(UnitId("unit.missing"))).isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
    }

    @Test
    fun getStages_databaseCannotOpen_unavailableAndLogsTheCause() = runTest {
        val broken = RoomCourseRepository { throw IllegalStateException("Pre-packaged database has an invalid schema") }
        val logged = mutableListOf<Throwable?>()
        val tree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                logged += t
            }
        }
        Timber.plant(tree)
        try {
            assertThat(broken.getStages()).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
        } finally {
            Timber.uproot(tree)
        }
        assertThat(logged.single()).isNotNull()
    }
}
