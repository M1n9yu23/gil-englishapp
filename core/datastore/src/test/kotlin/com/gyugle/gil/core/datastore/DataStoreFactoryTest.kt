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
package com.gyugle.gil.core.datastore

import androidx.datastore.preferences.core.intPreferencesKey
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir

class DataStoreFactoryTest {

    @TempDir
    lateinit var tempDir: File

    private val minutes = intPreferencesKey("minutes")

    private fun factory(directory: File = File(tempDir, "datastore")) = DataStoreFactory { directory }

    @Test
    fun create_validName_writesToNamedFileUnderDirectory() = runTest {
        val store = factory().create("goal_prefs")

        assertThat(store.editCatching { it[minutes] = 15 }).isEqualTo(Outcome.Success(Unit))

        assertThat(File(tempDir, "datastore/goal_prefs.preferences_pb").exists()).isTrue()
        assertThat(store.dataOrEmpty().first()[minutes]).isEqualTo(15)
    }

    @Test
    fun create_sameNameTwice_throwsNamingTheFile() {
        val factory = factory()
        factory.create("goal_prefs")

        assertFailure { factory.create("goal_prefs") }
            .isInstanceOf<IllegalStateException>()
            .hasMessage("DataStore file 'goal_prefs' is already registered")
    }

    @Test
    fun editCatching_directoryCannotBeCreated_unknownError() = runTest {
        val blocked = File(tempDir, "datastore").apply { writeText("a file where the directory should be") }
        val store = factory(blocked).create("goal_prefs")

        assertThat(store.editCatching { it[minutes] = 15 }).isEqualTo(Outcome.Failure(DataError.Local.UNKNOWN))
    }

    // Replacing the damaged file renames over it, which java.io.File cannot do on Windows.
    @Test
    @DisabledOnOs(OS.WINDOWS)
    fun create_damagedFile_readsEmptyAndAcceptsNewWrites() = runTest {
        val directory = File(tempDir, "datastore").apply { mkdirs() }
        File(directory, "goal_prefs.preferences_pb").writeBytes(byteArrayOf(0x7f, 0x00, 0x13))
        val store = factory(directory).create("goal_prefs")

        assertThat(store.dataOrEmpty().first()[minutes]).isNull()
        assertThat(store.editCatching { it[minutes] = 20 }).isEqualTo(Outcome.Success(Unit))
        assertThat(store.dataOrEmpty().first()[minutes]).isEqualTo(20)
    }
}
