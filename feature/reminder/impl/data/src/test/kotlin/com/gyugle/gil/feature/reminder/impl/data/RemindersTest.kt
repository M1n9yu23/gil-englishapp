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
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.single
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.Mistake
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.reminder.impl.data.repository.DataStoreReminderRepository
import java.io.File
import java.nio.file.Files
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
    private val today = StudyDay(20_399)
    private val progress = FakeProgressObserver(today)
    private val reminders = Reminders(
        repository = repository,
        notifier = notifier,
        goalObserver = FakeGoalObserver(DailyGoal.TWENTY),
        progressObserver = progress,
        clock = FixedStudyClock(today)
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

    @Test
    fun remind_goalNotMetWithReviewsDue_showsTheReviews() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))
        progress.studied = 5.minutes
        progress.dueCount = 12

        reminders.remind()

        assertThat(posted()).single().transform { it.extras.getCharSequence(Notification.EXTRA_TEXT).toString() }
            .isEqualTo("복습 12개, 약 6분")
    }

    @Test
    fun remind_goalMet_showsNothingButKeepsTheNextAlarm() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))
        alarmManager.cancelAll()
        progress.studied = 20.minutes
        progress.dueCount = 12

        reminders.remind()

        assertThat(posted()).isEmpty()
        assertThat(alarmCount()).isEqualTo(1)
    }

    @Test
    fun remind_readsOnlyTodaysStudyTime() = runTest {
        repository.setTimes(listOf(ReminderTime(20, 0)))

        reminders.remind()

        assertThat(progress.studyTimeRequests).containsExactly(today to today)
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

    private class FakeGoalObserver(private val goal: DailyGoal) : GoalObserver {
        override fun observeGoal(): Flow<DailyGoal> = flowOf(goal)
    }

    private class FixedStudyClock(private val day: StudyDay) : StudyClock {
        override fun now(): Instant = Instant.parse("2026-11-07T11:00:00Z")

        override fun dayOf(instant: Instant): StudyDay = day

        override fun startOf(day: StudyDay): Instant = error("Reminders do not ask when a day starts")
    }

    /** Only what a reminder reads; the rest is not part of these tests. */
    private class FakeProgressObserver(private val today: StudyDay) : ProgressObserver {
        var studied: Duration = Duration.ZERO
        var dueCount = 0
        var failure: Exception? = null
        val studyTimeRequests = mutableListOf<Pair<StudyDay, StudyDay>>()

        override fun observeStudyTime(from: StudyDay, to: StudyDay): Flow<List<DailyStudyTime>> = flow {
            failure?.let { throw it }
            studyTimeRequests += from to to
            emit(if (studied > Duration.ZERO) listOf(DailyStudyTime(today, studied)) else emptyList())
        }

        override fun observeDueCards(day: StudyDay): Flow<List<CardState>> = flowOf(
            List(dueCount) { index ->
                CardState(SenseId("sense.word$index.noun.thing"), 1.0, day, day - 1, 1, 0)
            }
        )

        override fun observeCard(itemId: StudyItemId): Flow<CardState?> = error("Not used by reminders")

        override fun observeCards(): Flow<List<CardState>> = error("Not used by reminders")

        override fun observeCompletedLessons(): Flow<Set<LessonId>> = error("Not used by reminders")

        override fun observeStreak(today: StudyDay): Flow<Int> = error("Not used by reminders")

        override fun observeSavedItems(): Flow<List<StudyItemId>> = error("Not used by reminders")

        override fun observeMistakes(): Flow<List<Mistake>> = error("Not used by reminders")
    }
}
