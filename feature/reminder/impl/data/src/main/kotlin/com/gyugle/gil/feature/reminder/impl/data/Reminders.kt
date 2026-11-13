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

import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.reminder.impl.data.repository.DataStoreReminderRepository
import com.gyugle.gil.feature.reminder.impl.domain.model.ReminderMessage
import com.gyugle.gil.feature.reminder.impl.domain.reminderMessageFor
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import timber.log.Timber

/**
 * What app start and the receivers do. Runs in the application scope, where an exception would
 * crash the app, so each step catches its own failure and logs only the exception's type.
 */
internal class Reminders @Inject constructor(
    private val repository: DataStoreReminderRepository,
    private val notifier: ReminderNotifier,
    private val goalObserver: GoalObserver,
    private val progressObserver: ProgressObserver,
    private val clock: StudyClock
) {
    /** At app start: sets only the alarms that are missing, so today's pending ones keep their time. */
    suspend fun restoreAlarms() {
        reschedule(keepPending = true)
    }

    /** After a restart, a clock or time zone change, or an update: every alarm from now. */
    suspend fun rescheduleAll() {
        reschedule(keepPending = false)
    }

    /**
     * A reminder time has come. Sets the next alarm, then shows today's reminder unless reminders
     * were just turned off, the goal is already met or the alarm arrived long after [dueAt].
     */
    suspend fun remind(dueAt: Instant? = null) {
        // Failing to set the next alarm is no reason to drop this one.
        val times = reschedule(keepPending = false)
        if (times != null && times.isEmpty()) return
        // An alarm the clock jumped past fires at once; a reminder hours off its time only confuses.
        if (dueAt != null && clock.now() - dueAt > LATE_LIMIT) return
        val message = attempt("Could not read today's record for the reminder") { todayMessage() } ?: return
        notifier.show(message)
    }

    /** The saved times, or null when they could not be read or set. */
    private suspend fun reschedule(keepPending: Boolean): List<ReminderTime>? =
        attempt("Could not set the reminder alarms") { repository.reschedule(keepPending) }

    private suspend fun todayMessage(): ReminderMessage? {
        val today = clock.today()
        val goal = goalObserver.observeGoal().first()
        val studied = progressObserver.observeStudyTime(today, today).first()
            .fold(Duration.ZERO) { total, day -> total + day.duration }
        val due = progressObserver.observeDueCards(today).first().size
        return reminderMessageFor(goal, studied, due)
    }

    private suspend fun <T> attempt(what: String, block: suspend () -> T): T? = try {
        block()
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        Timber.w("%s: %s", what, e::class.simpleName)
        null
    }

    private companion object {
        /** Doze can hold an alarm for a while; past this, the reminder is skipped. */
        val LATE_LIMIT = 2.hours
    }
}
