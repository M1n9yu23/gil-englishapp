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
package com.gyugle.gil.feature.goal.impl.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.goal.api.DailyGoal
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreGoalRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private fun store(file: File = File(tempDir, "goal_prefs.preferences_pb")) =
        PreferenceDataStoreFactory.create { file }

    @Test
    fun observeGoal_nothingSaved_emitsDefault() = runTest {
        val repository = DataStoreGoalRepository(store())

        assertThat(repository.observeGoal().first()).isEqualTo(DailyGoal.DEFAULT)
    }

    @Test
    fun setGoal_newGoal_isObservedAndKeptInTheFile() = runTest {
        val file = File(tempDir, "goal_prefs.preferences_pb")
        val repository = DataStoreGoalRepository(store(file))

        repository.observeGoal().test {
            assertThat(awaitItem()).isEqualTo(DailyGoal.TWENTY)
            assertThat(repository.setGoal(DailyGoal.SIXTY)).isEqualTo(Outcome.Success(Unit))
            assertThat(awaitItem()).isEqualTo(DailyGoal.SIXTY)
        }
        assertThat(file.exists()).isTrue()
    }

    @Test
    fun setGoal_sameGoalAgain_emitsNothingNew() = runTest {
        val repository = DataStoreGoalRepository(store())
        repository.setGoal(DailyGoal.TEN)

        repository.observeGoal().test {
            assertThat(awaitItem()).isEqualTo(DailyGoal.TEN)
            repository.setGoal(DailyGoal.TEN)
            expectNoEvents()
        }
    }

    @Test
    fun observeGoal_savedMinutesMatchNoGoal_emitsDefault() = runTest {
        val store = store()
        store.edit { it[intPreferencesKey("daily_goal_minutes")] = 45 }

        assertThat(DataStoreGoalRepository(store).observeGoal().first()).isEqualTo(DailyGoal.DEFAULT)
    }

    @Test
    fun setGoal_fileCannotBeWritten_unknownError() = runTest {
        val blocked = File(tempDir, "datastore").apply { writeText("a file where the directory should be") }
        val repository = DataStoreGoalRepository(store(File(blocked, "goal_prefs.preferences_pb")))

        assertThat(repository.setGoal(DailyGoal.THIRTY)).isEqualTo(Outcome.Failure(DataError.Local.UNKNOWN))
    }
}
