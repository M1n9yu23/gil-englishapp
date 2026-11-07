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
package com.gyugle.gil.feature.onboarding.impl.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreOnboardingRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private fun repository(file: File = File(tempDir, "onboarding_prefs.preferences_pb")) =
        DataStoreOnboardingRepository(PreferenceDataStoreFactory.create { file })

    @Test
    fun observeCompleted_firstRun_isFalse() = runTest {
        assertThat(repository().observeCompleted().first()).isFalse()
    }

    @Test
    fun complete_firstRun_emitsTrue() = runTest {
        val repository = repository()

        repository.observeCompleted().test {
            assertThat(awaitItem()).isFalse()
            assertThat(repository.complete()).isEqualTo(Outcome.Success(Unit))
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun complete_fileCannotBeWritten_unknownErrorAndStillFirstRun() = runTest {
        val blocked = File(tempDir, "datastore").apply { writeText("a file where the directory should be") }
        val repository = repository(File(blocked, "onboarding_prefs.preferences_pb"))

        assertThat(repository.complete()).isEqualTo(Outcome.Failure(DataError.Local.UNKNOWN))
        assertThat(repository.observeCompleted().first()).isFalse()
    }
}
