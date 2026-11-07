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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gyugle.gil.core.datastore.dataOrEmpty
import com.gyugle.gil.core.datastore.editCatching
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.feature.reminder.api.ReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderObserver
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.reminder.impl.data.ReminderAlarms
import com.gyugle.gil.feature.reminder.impl.data.di.ReminderPreferences
import com.gyugle.gil.feature.reminder.impl.domain.reminderTimesToSave
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Keeps the reminder times as `HH:mm` text, such as `08:00,20:30`, and keeps the alarms in step
 * with what is saved.
 */
@Singleton
internal class DataStoreReminderRepository @Inject constructor(
    @param:ReminderPreferences private val store: DataStore<Preferences>,
    private val alarms: ReminderAlarms
) : ReminderObserver,
    ReminderCommands {

    // A reschedule that read the old times must not set its alarms after a save set the new ones.
    private val scheduling = Mutex()

    override fun observeTimes(): Flow<List<ReminderTime>> = store.dataOrEmpty()
        .map { preferences -> parseTimes(preferences[TIMES]) }
        .distinctUntilChanged()

    override suspend fun setTimes(times: List<ReminderTime>): EmptyOutcome<DataError.Local> {
        val saved = reminderTimesToSave(times)
        // The file write finishes even when the caller is cancelled, so the alarms must follow it.
        return withContext(NonCancellable) {
            scheduling.withLock {
                store.editCatching { it[TIMES] = saved.joinToString(SEPARATOR) { time -> time.format() } }
                    .onSuccess { alarms.schedule(saved) }
            }
        }
    }

    /**
     * Sets the alarms for the saved times again and returns those times. With [keepPending], only
     * the alarms that are missing, as [ReminderAlarms.schedule] explains.
     */
    suspend fun reschedule(keepPending: Boolean = false): List<ReminderTime> = scheduling.withLock {
        observeTimes().first().also { times -> alarms.schedule(times, keepPending) }
    }

    private companion object {
        val TIMES = stringPreferencesKey("reminder_times")
        const val SEPARATOR = ","
        val TIME_FORMAT = Regex("""(\d{2}):(\d{2})""")

        // Not String.format: some locales write other digits, which the parser would not read back.
        fun ReminderTime.format(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

        /** Skips anything unreadable, so a damaged value turns that reminder off instead of crashing. */
        fun parseTimes(value: String?): List<ReminderTime> {
            if (value.isNullOrEmpty()) return emptyList()
            val times = value.split(SEPARATOR).mapNotNull { text ->
                val match = TIME_FORMAT.matchEntire(text) ?: return@mapNotNull null
                val (hour, minute) = match.destructured
                ReminderTime.ofOrNull(hour.toInt(), minute.toInt())
            }
            return times.distinct().sorted().take(ReminderTime.MAX_PER_DAY)
        }

        fun ReminderTime.Companion.ofOrNull(hour: Int, minute: Int): ReminderTime? =
            if (hour in 0..23 && minute in 0..59) ReminderTime(hour, minute) else null
    }
}
