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
package com.gyugle.gil.feature.reminder.impl.data.repository

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.reminder.impl.data.ReminderAlarms
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class DataStoreReminderRepositoryTest {
    private val context = RuntimeEnvironment.getApplication()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val directory: File = Files.createTempDirectory("reminder").toFile()
    private val file = File(directory, "reminder_prefs.preferences_pb")
    private val store = PreferenceDataStoreFactory.create { file }
    private val repository = DataStoreReminderRepository(store, ReminderAlarms(context))

    @After
    fun deleteFiles() {
        directory.deleteRecursively()
    }

    private fun alarmCount() = shadowOf(alarmManager).scheduledAlarms.size

    @Test
    fun observeTimes_nothingSaved_isEmpty() = runTest {
        assertThat(repository.observeTimes().first()).isEmpty()
    }

    @Test
    fun setTimes_twoTimes_savedInDayOrderWithAnAlarmEach() = runTest {
        repository.observeTimes().test {
            assertThat(awaitItem()).isEmpty()
            val result = repository.setTimes(listOf(ReminderTime(20, 30), ReminderTime(8, 5)))

            assertThat(result).isEqualTo(Outcome.Success(Unit))
            assertThat(awaitItem()).containsExactly(ReminderTime(8, 5), ReminderTime(20, 30))
        }
        assertThat(alarmCount()).isEqualTo(2)
    }

    @Test
    fun setTimes_empty_turnsRemindersOff() = runTest {
        repository.setTimes(listOf(ReminderTime(8, 0)))
        repository.setTimes(emptyList())

        assertThat(repository.observeTimes().first()).isEmpty()
        assertThat(alarmCount()).isEqualTo(0)
    }

    @Test
    fun setTimes_threeTimes_throwsAndKeepsTheSavedTimes() = runTest {
        repository.setTimes(listOf(ReminderTime(8, 0)))

        assertFailure {
            repository.setTimes(listOf(ReminderTime(7, 0), ReminderTime(12, 0), ReminderTime(20, 0)))
        }.isInstanceOf<IllegalArgumentException>()
        assertThat(repository.observeTimes().first()).containsExactly(ReminderTime(8, 0))
    }

    @Test
    fun setTimes_fileCannotBeWritten_unknownErrorAndNoAlarm() = runTest {
        val blocked = File(directory, "blocked").apply { writeText("a file where the directory should be") }
        val repository = DataStoreReminderRepository(
            PreferenceDataStoreFactory.create { File(blocked, "reminder_prefs.preferences_pb") },
            ReminderAlarms(context)
        )

        assertThat(repository.setTimes(listOf(ReminderTime(8, 0)))).isEqualTo(Outcome.Failure(DataError.Local.UNKNOWN))
        assertThat(alarmCount()).isEqualTo(0)
    }

    @Test
    fun observeTimes_damagedValue_keepsOnlyReadableTimes() = runTest {
        store.edit { it[stringPreferencesKey("reminder_times")] = "25:00,8:00,07:30,abc,07:30,21:15,22:00" }

        assertThat(repository.observeTimes().first()).containsExactly(ReminderTime(7, 30), ReminderTime(21, 15))
    }

    @Test
    fun reschedule_savedTimes_setsTheirAlarmsAgain() = runTest {
        repository.setTimes(listOf(ReminderTime(8, 0), ReminderTime(20, 0)))
        alarmManager.cancelAll()

        repository.reschedule()

        assertThat(alarmCount()).isEqualTo(2)
    }
}
