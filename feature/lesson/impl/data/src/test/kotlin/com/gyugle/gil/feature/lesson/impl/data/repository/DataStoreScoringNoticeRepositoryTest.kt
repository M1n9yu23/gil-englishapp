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
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.Outcome
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreScoringNoticeRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private val store by lazy { PreferenceDataStoreFactory.create { File(tempDir, "lesson_prefs.preferences_pb") } }
    private val repository by lazy { DataStoreScoringNoticeRepository(store) }

    @Test
    fun wasShown_nothingSaved_isFalse() = runTest {
        assertThat(repository.wasShown()).isFalse()
    }

    @Test
    fun markShown_thenWasShown_isTrue() = runTest {
        assertThat(repository.markShown()).isEqualTo(Outcome.Success(Unit))

        assertThat(repository.wasShown()).isTrue()
    }
}
