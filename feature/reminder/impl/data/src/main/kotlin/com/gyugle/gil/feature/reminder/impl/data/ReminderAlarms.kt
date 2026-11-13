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

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.reminder.impl.domain.nextReminderAt
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.TimeZone

/**
 * One inexact alarm per reminder time, through [AlarmManager.setWindow].
 *
 * While the phone is in use a reminder comes up to [WINDOW] late; Doze and App Standby can defer
 * it further. A study reminder can afford that, and in return the app needs no exact alarm
 * permission. Each alarm fires once; [ReminderAlarmReceiver] sets the next one. Alarms do not
 * survive a restart or a force stop, so the app sets them again at boot and at every start.
 */
internal class ReminderAlarms(
    private val context: Context,
    private val clock: Clock,
    private val zone: () -> TimeZone
) {
    @Inject
    constructor(@ApplicationContext context: Context) : this(context, Clock.System, TimeZone::currentSystemDefault)

    private val alarmManager: AlarmManager = checkNotNull(context.getSystemService<AlarmManager>())

    /**
     * Sets one alarm for each of [times] and cancels the alarms of times that are gone.
     *
     * With [keepPending], a slot that still has its alarm keeps it. An alarm can arrive minutes
     * after its time, and setting it again from now would move today's reminder to tomorrow.
     */
    fun schedule(times: List<ReminderTime>, keepPending: Boolean = false) {
        val now = clock.now()
        val currentZone = zone()
        repeat(ReminderTime.MAX_PER_DAY) { slot ->
            val time = times.getOrNull(slot)
            if (time == null) {
                // Cancelling the pending intent too, so that hasAlarm sees the slot as empty.
                val operation = alarmIntent(slot)
                alarmManager.cancel(operation)
                operation.cancel()
            } else if (!keepPending || !hasAlarm(slot)) {
                val triggerAt = nextReminderAt(time, now, currentZone).toEpochMilliseconds()
                alarmManager.setWindow(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    WINDOW.inWholeMilliseconds,
                    alarmIntent(slot, dueAt = triggerAt)
                )
            }
        }
    }

    /** A restart and a force stop clear the pending intents along with the alarms. */
    private fun hasAlarm(slot: Int): Boolean = PendingIntent.getBroadcast(
        context,
        slot,
        alarmReceiverIntent(),
        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    ) != null

    // The slot is the request code, so the two alarms are two different pending intents.
    // The time rides along as an extra, which the pending intent's identity ignores.
    private fun alarmIntent(slot: Int, dueAt: Long? = null): PendingIntent = PendingIntent.getBroadcast(
        context,
        slot,
        alarmReceiverIntent().apply { if (dueAt != null) putExtra(EXTRA_DUE_AT, dueAt) },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun alarmReceiverIntent() = Intent(context, ReminderAlarmReceiver::class.java)

    companion object {
        /** Android stretches any shorter window to ten minutes on recent versions. */
        val WINDOW = 10.minutes

        /** Epoch milliseconds the alarm was set for. */
        const val EXTRA_DUE_AT = "due_at"
    }
}
