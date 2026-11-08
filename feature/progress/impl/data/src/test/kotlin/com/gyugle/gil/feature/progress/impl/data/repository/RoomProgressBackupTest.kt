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
package com.gyugle.gil.feature.progress.impl.data.repository

import android.app.Application
import androidx.room.Room
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEqualTo
import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.ProgressBackupPreview
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.impl.data.ProgressDatabase
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Restores the committed example of each format version and exports it again. A change that
 * breaks a saved file fails here; a new format version adds its own example file.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomProgressBackupTest {
    private val database = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        ProgressDatabase::class.java
    ).build()
    private val backup = RoomProgressBackup(database)
    private val repository =
        RoomProgressRepository(database, FakeStudyClock(Instant.parse("2026-11-07T03:00:00Z")), FsrsScheduler())
    private val hello = SenseId("sense.hello.interj.greeting")
    private val letterA = SoundId("sound.letter-a")
    private val versionOne = readResource("backup/progress-v1.json")

    @After
    fun close() = database.close()

    @Test
    fun restoreThenExport_versionOneFile_givesTheSameRecordBack() = runTest {
        assertThat(backup.restore(versionOne)).isEqualTo(Outcome.Success(Unit))

        val exported = (backup.export() as Outcome.Success).data

        assertThat(Json.parseToJsonElement(exported)).isEqualTo(Json.parseToJsonElement(versionOne))
    }

    @Test
    fun exportThenRestore_recordWrittenByTheApp_comesBackWhole() = runTest {
        repository.completeLesson(
            LessonId("lesson.s1-15.1"),
            mapOf(
                hello to ReviewRating.GOOD,
                letterA to ReviewRating.HARD
            )
        )
        repository.recordReview(letterA, ReviewRating.AGAIN)
        repository.recordStudyTime(7.minutes)
        repository.setSaved(hello, saved = true)
        repository.recordMistake(letterA)
        val cards = repository.observeCards().first()
        val exported = (backup.export() as Outcome.Success).data
        backup.restore(EMPTY_RECORD)
        assertThat(repository.observeCards().first()).isEqualTo(emptyList())

        assertThat(backup.restore(exported)).isEqualTo(Outcome.Success(Unit))

        assertThat(repository.observeCards().first()).isEqualTo(cards)
        assertThat(repository.observeSavedItems().first()).isEqualTo(listOf(hello))
        assertThat(repository.observeMistakes().first().map { it.itemId }).isEqualTo(listOf(letterA))
        assertThat(
            Json.parseToJsonElement((backup.export() as Outcome.Success).data)
        ).isEqualTo(Json.parseToJsonElement(exported))
    }

    @Test
    fun export_rowsARestoreWouldReject_areLeftOut() = runTest {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO lesson_completion (lessonId, completedAt, studyDay) VALUES ('not-a-lesson', 0, 0)"
        )
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO saved_item (itemId, savedAt) VALUES ('word.hello', 0)"
        )

        val exported = (backup.export() as Outcome.Success).data

        assertThat(backup.restore(exported)).isEqualTo(Outcome.Success(Unit))
        assertThat(Json.parseToJsonElement(exported)).isEqualTo(Json.parseToJsonElement(EMPTY_RECORD))
    }

    @Test
    fun restore_versionOneFile_isReadByTheRepository() = runTest {
        backup.restore(versionOne)

        assertThat(repository.observeCards().first().size).isEqualTo(3)
        assertThat(repository.observeSavedItems().first()).isEqualTo(listOf(SenseId("sense.hello.interj.greeting")))
    }

    @Test
    fun restore_replacesTheWholeRecord() = runTest {
        repository.recordReview(SenseId("sense.bye.interj.leaving"), ReviewRating.GOOD)

        backup.restore(versionOne)

        assertThat(repository.observeCard(SenseId("sense.bye.interj.leaving")).first()).isEqualTo(null)
    }

    @Test
    fun clear_emptiesEveryTable() = runTest {
        backup.restore(versionOne)

        backup.clear()

        val exported = (backup.export() as Outcome.Success).data
        assertThat(Json.parseToJsonElement(exported)).isEqualTo(Json.parseToJsonElement(EMPTY_RECORD))
    }

    @Test
    fun export_emptyRecord_writesVersionAndEmptyLists() = runTest {
        val exported = (backup.export() as Outcome.Success).data

        assertThat(Json.parseToJsonElement(exported)).isEqualTo(
            Json.parseToJsonElement(
                EMPTY_RECORD
            )
        )
    }

    @Test
    fun preview_versionOneFile_countsItemsAndLessonsWithoutWriting() = runTest {
        val preview = backup.preview(versionOne)

        assertThat(preview).isEqualTo(
            Outcome.Success(
                ProgressBackupPreview(
                    itemCount = 3,
                    completedLessonCount = 2,
                    lastStudyDay = StudyDay(LocalDate.parse("2026-11-06").toEpochDays())
                )
            )
        )
        assertThat(repository.observeCards().first()).isEqualTo(emptyList())
    }

    @Test
    fun restore_newerVersion_isRejectedAsNewerAndChangesNothing() = runTest {
        repository.recordReview(SenseId("sense.bye.interj.leaving"), ReviewRating.GOOD)

        val result = backup.restore("""{"version":2,"somethingNew":[]}""")

        assertThat(result).isEqualTo(Outcome.Failure(BackupError.Format.NEWER_VERSION))
        assertThat(repository.observeCards().first().size).isEqualTo(1)
    }

    @Test
    fun restore_brokenFiles_areRejectedAsMalformedAndChangeNothing() = runTest {
        repository.recordReview(SenseId("sense.bye.interj.leaving"), ReviewRating.GOOD)
        val broken = listOf(
            "",
            "not json",
            "[]",
            """{"cards":[]}""",
            """{"version":0}""",
            """{"version":1}""",
            versionOne.replace("\"stability\": 3.17", "\"stability\": -1.0"),
            versionOne.replace(Regex("\"difficulty\": 2.118,(\\s+)\"dueDay\""), "\"difficulty\": 11.0,$1\"dueDay\""),
            // The pattern's card, not its review: only the card has stability right after the item.
            versionOne.replace(Regex("\"pattern.how-are-you\",(\\s+)\"stability\""), "\"word.hello\",$1\"stability\""),
            versionOne.replace(
                Regex("\"pattern.how-are-you\",(\\s+)\"stability\""),
                "\"sound.letter-a\",$1\"stability\""
            ),
            versionOne.replace("\"rating\": \"hard\"", "\"rating\": \"easy\""),
            versionOne.replace("\"lesson\": \"lesson.s1-15.2\"", "\"lesson\": \"unit.s1-15\""),
            versionOne.replace("\"dueDay\": \"2026-11-09\"", "\"dueDay\": \"2026-11-31\""),
            versionOne.replace("\"version\": 1,", "\"version\": 1, \"extra\": true,"),
            versionOne.replace("\"lapseCount\": 1", "\"lapseCount\": 2"),
            versionOne.replace("\"dueDay\": \"2026-11-07\"", "\"dueDay\": \"2026-11-05\""),
            versionOne.replace("\"count\": 2", "\"count\": 0"),
            versionOne.replace("\"millis\": 720000", "\"millis\": 90000001"),
            versionOne.replace("\"day\": \"2026-11-06\", \"millis\"", "\"day\": \"2026-11-05\", \"millis\""),
            // The saved word twice.
            versionOne.replace(Regex("""(\{ "item": "sense\.hello[^}]*})(\s*])"""), "$1, $1$2")
        )

        broken.forEach { section ->
            assertThat(section, "a replacement that matched nothing").isNotEqualTo(versionOne)
            assertThat(
                backup.restore(section),
                section.take(80)
            ).isEqualTo(Outcome.Failure(BackupError.Format.MALFORMED))
            assertThat(backup.preview(section), section.take(80)).isInstanceOf(Outcome.Failure::class)
        }
        assertThat(repository.observeCards().first().size).isEqualTo(1)
    }

    private companion object {
        const val EMPTY_RECORD =
            """{"version":1,"cards":[],"reviews":[],"lessons":[],"studyTime":[],"saved":[],"mistakes":[]}"""
    }

    private fun readResource(path: String): String =
        checkNotNull(javaClass.classLoader?.getResource(path)) { "Missing test resource $path" }.readText()
}
