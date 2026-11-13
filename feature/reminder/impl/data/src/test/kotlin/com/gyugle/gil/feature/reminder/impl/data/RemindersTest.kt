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
package com.gyugle.gil.feature.reminder.impl.data

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.single
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.FakeGoalRepository
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.reminder.impl.data.repository.DataStoreReminderRepository
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "ko")
class RemindersTest {
    private val context = RuntimeEnvironment.getApplication()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val directory: File = Files.createTempDirectory("reminders").toFile()
    private val repository = DataStoreReminderRepository(
        PreferenceDataStoreFactory.create { File(directory, "reminder_prefs.preferences_pb") },
        ReminderAlarms(context)
    )
    private val notifier = ReminderNotifier(context)
    private val clock = FakeStudyClock()
    private val today = clock.dayOf(clock.now())
    private val progress = FakeProgressObserver()
    private val reminders = Reminders(
        repository = repository,
        notifier = notifier,
        goalObserver = FakeGoalRepository(DailyGoal.TWENTY),
        progressObserver = progress,
        clock = clock
    )

    @Before
    fun setUp() {
        notifier.createChannel()
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun deleteFiles() {
        directory.deleteRecursively()
    }

    private fun posted(): List<Notification> = shadowOf(notificationManager).allNotifications

    private fun alarmCount() = shadowOf(alarmManager).scheduledAlarms.size

    private fun studied(day: StudyDay, time: Duration) {
        progress.studyTime.value += DailyStudyTime(day, time)
    }

    private fun due(count: Int) {
        progress.cards.value = List(count) { index ->
            CardState(SenseId("sense.word$index.n.thing"), 1.0, today, today - 1, 1, 0)
        }
    }

    @Test
    fun remind_goalNotMetWithReviewsDue_showsTheReviews() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))
        studied(today, 5.minutes)
        due(12)

        reminders.remind()

        assertThat(posted()).single().transform { it.extras.getCharSequence(Notification.EXTRA_TEXT).toString() }
            .isEqualTo("복습 12개, 약 6분")
    }

    @Test
    fun remind_arrivingAnHourLate_stillReminds() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))

        reminders.remind(dueAt = clock.now() - 1.hours)

        assertThat(posted()).hasSize(1)
    }

    @Test
    fun remind_arrivingExactlyTwoHoursLate_stillReminds() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))

        reminders.remind(dueAt = clock.now() - 2.hours)

        assertThat(posted()).hasSize(1)
    }

    @Test
    fun remind_arrivingJustOverTwoHoursLate_showsNothing() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))

        reminders.remind(dueAt = clock.now() - 2.hours - 1.milliseconds)

        assertThat(posted()).isEmpty()
    }

    // Setting the clock forward past a reminder time fires that alarm at once.
    @Test
    fun remind_arrivingHoursLate_showsNothingButKeepsTheNextAlarm() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))
        alarmManager.cancelAll()

        reminders.remind(dueAt = clock.now() - 26.hours)

        assertThat(posted()).isEmpty()
        assertThat(alarmCount()).isEqualTo(1)
    }

    @Test
    fun remind_goalMet_showsNothingButKeepsTheNextAlarm() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))
        alarmManager.cancelAll()
        studied(today, 20.minutes)
        due(12)

        reminders.remind()

        assertThat(posted()).isEmpty()
        assertThat(alarmCount()).isEqualTo(1)
    }

    @Test
    fun remind_goalMetOnlyYesterday_stillReminds() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))
        studied(today - 1, 30.minutes)

        reminders.remind()

        assertThat(posted()).hasSize(1)
    }

    @Test
    fun remind_remindersJustTurnedOff_showsNothing() = runTest {
        repository.setTimes(emptyList())

        reminders.remind()

        assertThat(posted()).isEmpty()
    }

    @Test
    fun remind_recordCannotBeRead_staysQuietAndKeepsTheNextAlarm() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))
        alarmManager.cancelAll()
        progress.failure = IllegalStateException("database is closed")

        reminders.remind()

        assertThat(posted()).isEmpty()
        assertThat(alarmCount()).isEqualTo(1)
    }

    @Test
    fun restoreAlarms_afterForceStop_setsTheMissingAlarms() = runTest {
        repository.setTimes(listOf(ReminderTime(8, 0), ReminderTime(20, 0)))
        // A force stop clears the alarms and their pending intents.
        alarmManager.cancelAll()
        ReminderAlarms(context).schedule(emptyList())

        reminders.restoreAlarms()

        assertThat(alarmCount()).isEqualTo(2)
    }
}
