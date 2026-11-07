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
package com.gyugle.gil.feature.lesson.impl.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir

class DataStoreLessonRunRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private val one = LessonId("lesson.s1-01.1")
    private val two = LessonId("lesson.s1-01.2")
    private val store by lazy { PreferenceDataStoreFactory.create { File(tempDir, "lesson_prefs.preferences_pb") } }
    private val repository by lazy { DataStoreLessonRunRepository(store) }

    private val halfway = LessonRun.start(stepCount = 4, skipped = setOf(3))
        .answer(AnswerGrade.WRONG).next()
        .answer(AnswerGrade.CLOSE).next()
        .studied(42_000)

    @Test
    fun get_nothingSaved_isNull() = runTest {
        assertThat(repository.get(one)).isNull()
    }

    @Test
    fun save_thenGet_returnsTheSameRun() = runTest {
        assertThat(repository.save(one, halfway)).isEqualTo(Outcome.Success(Unit))

        assertThat(repository.get(one)).isEqualTo(halfway)
        assertThat(repository.get(two)).isNull()
    }

    // A second write renames over the file, which java.io.File cannot do on Windows.
    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun save_twoLessons_keepsEachSeparately() = runTest {
        val started = LessonRun.start(stepCount = 2)
        repository.save(one, halfway)
        repository.save(two, started)

        assertThat(repository.get(one)).isEqualTo(halfway)
        assertThat(repository.get(two)).isEqualTo(started)
    }

    // A second write renames over the file, which java.io.File cannot do on Windows.
    @DisabledOnOs(OS.WINDOWS)
    @Test
    fun clear_savedLesson_forgetsIt() = runTest {
        repository.save(one, halfway)

        assertThat(repository.clear(one)).isEqualTo(Outcome.Success(Unit))

        assertThat(repository.get(one)).isNull()
    }

    @Test
    fun clear_anotherLesson_keepsThisOne() = runTest {
        repository.save(one, halfway)

        assertThat(repository.clear(two)).isEqualTo(Outcome.Success(Unit))

        assertThat(repository.get(one)).isEqualTo(halfway)
    }

    @Test
    fun get_unreadableEntry_isNull() = runTest {
        store.edit { it[stringPreferencesKey("run.${one.value}")] = "{not json" }

        assertThat(repository.get(one)).isNull()
    }

    @Test
    fun get_entryPointingPastItsSteps_isNull() = runTest {
        store.edit {
            it[stringPreferencesKey("run.${one.value}")] =
                """{"stepCount":2,"firstPassSize":2,"queue":[0,5],"position":0,"outcomes":{},"studiedMillis":0}"""
        }

        assertThat(repository.get(one)).isNull()
    }

    @Test
    fun save_fileCannotBeWritten_unknownError() = runTest {
        val blocked = File(tempDir, "datastore").apply { writeText("a file where the directory should be") }
        val repository = DataStoreLessonRunRepository(
            PreferenceDataStoreFactory.create { File(blocked, "lesson_prefs.preferences_pb") }
        )

        assertThat(repository.save(one, halfway)).isEqualTo(Outcome.Failure(DataError.Local.UNKNOWN))
        assertThat(repository.get(one)).isNull()
    }
}
