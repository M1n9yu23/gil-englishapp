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
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.Choice
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabase
import com.gyugle.gil.feature.curriculum.impl.domain.model.LessonSummary
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
                execSQL(
                    "INSERT INTO word (id, text, ipa, hangul, searchKey) VALUES ('word.read', 'read', 'riːd', NULL, ' read')"
                )
                execSQL(
                    "INSERT INTO sense (id, wordId, position, pos, meaningKo, definitionEn, ipa, stageId, lessonId) " +
                        "VALUES ('sense.read.v.past', 'word.read', 1, 'verb', '읽었다', NULL, 'rɛd', 'stage.one', 'lesson.a.1')"
                )
                execSQL(
                    "INSERT INTO sense (id, wordId, position, pos, meaningKo, definitionEn, ipa, stageId, lessonId) " +
                        "VALUES ('sense.read.v.now', 'word.read', 0, 'verb', '읽다', NULL, NULL, 'stage.one', 'lesson.a.1')"
                )
                execSQL(
                    "INSERT INTO pattern (id, text, meaningKo, noteKo, stageId, lessonId) " +
                        "VALUES ('pattern.hi', 'Hi.', '안녕하세요', NULL, 'stage.one', 'lesson.a.1')"
                )
                execSQL(
                    "INSERT INTO sound (id, symbol, spelling, descriptionKo, stageId, lessonId) " +
                        "VALUES ('sound.f', 'f', 'f, ph', '설명', 'stage.one', 'lesson.a.1')"
                )
                // Out of order, and the sound first, to check the content's order comes back.
                item(position = 3, itemId = "sense.read.v.past")
                item(position = 0, itemId = "sound.f")
                item(position = 2, itemId = "pattern.hi")
                item(position = 1, itemId = "sense.read.v.now")
                step(position = 1, payload = """{"type":"shadow","sentence":{"id":2,"text":"Hi."}}""")
                step(
                    position = 0,
                    payload = """{"type":"listenChoose","audio":{"id":1,"text":"fan","ko":"부채"},""" +
                        """"options":[{"text":"f","english":true,"correct":true},""" +
                        """{"text":"p","english":true,"correct":false,"why":"입술이 붙어요."}],"item":"sound.f"}"""
                )
            }
        }
    private val repository = RoomCourseRepository { database }

    private fun androidx.sqlite.db.SupportSQLiteDatabase.item(position: Int, itemId: String) =
        execSQL("INSERT INTO lesson_item (lessonId, position, itemId) VALUES ('lesson.a.1', $position, '$itemId')")

    private fun androidx.sqlite.db.SupportSQLiteDatabase.step(position: Int, payload: String) =
        execSQL("INSERT INTO lesson_step (lessonId, position, payload) VALUES ('lesson.a.1', $position, '$payload')")

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
    fun getLesson_knownId_returnsStepsItemsInContentOrderAndTheNextLesson() = runTest {
        assertThat(repository.getLesson(LessonId("lesson.a.1"))).isEqualTo(
            Outcome.Success(
                Lesson(
                    id = LessonId("lesson.a.1"),
                    title = LocalizedText("lesson.a.1 ko", "lesson.a.1 en"),
                    goal = LocalizedText("lesson.a.1 목표", "lesson.a.1 goal"),
                    steps = listOf(
                        LessonStep.ListenChoose(
                            audio = Sentence(1, "fan", "부채"),
                            options = listOf(
                                Choice("f", english = true, correct = true, why = null),
                                Choice("p", english = true, correct = false, why = "입술이 붙어요.")
                            ),
                            item = SoundId("sound.f")
                        ),
                        LessonStep.Shadow(Sentence(2, "Hi.", ko = null))
                    ),
                    introduced = listOf(
                        LessonItem(SoundId("sound.f"), text = "f, ph", ipa = "f", meaningKo = null),
                        LessonItem(SenseId("sense.read.v.now"), text = "read", ipa = "riːd", meaningKo = "읽다"),
                        LessonItem(PatternId("pattern.hi"), text = "Hi.", ipa = null, meaningKo = "안녕하세요"),
                        // A sense said differently from its word keeps its own IPA.
                        LessonItem(SenseId("sense.read.v.past"), text = "read", ipa = "rɛd", meaningKo = "읽었다")
                    ),
                    next = LessonId("lesson.a.2")
                )
            )
        )
    }

    @Test
    fun getLesson_lastLessonOfTheCourse_hasNoNext() = runTest {
        val lesson = (repository.getLesson(LessonId("lesson.a.2")) as Outcome.Success).data

        assertThat(lesson.next).isNull()
        assertThat(lesson.steps).isEqualTo(emptyList())
    }

    @Test
    fun getLesson_unknownId_notFound() = runTest {
        assertThat(repository.getLesson(LessonId("lesson.missing"))).isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
    }

    @Test
    fun getLesson_itemMissingFromEveryTable_unavailable() = runTest {
        database.openHelper.writableDatabase.item(position = 9, itemId = "sense.gone")

        assertThat(repository.getLesson(LessonId("lesson.a.1"))).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }

    @Test
    fun getFirstLessonId_returnsTheLowestCoursePosition() = runTest {
        assertThat(repository.getFirstLessonId()).isEqualTo(Outcome.Success(LessonId("lesson.a.1")))
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
