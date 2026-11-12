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
package com.gyugle.gil.feature.widget.impl.presentation

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Updates the widget when a new study day begins, so the count and the sentence move on at 04:00.
 *
 * The alarm does not wake the phone: a widget nobody is looking at can wait, and the alarm goes off
 * as soon as the screen turns on. Every update sets it again, and a restart, which clears alarms,
 * also updates every widget.
 */
internal class DayRefreshAlarm @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val clock: StudyClock
) {
    private val alarmManager: AlarmManager = checkNotNull(context.getSystemService<AlarmManager>())

    /** Replaces the pending alarm with one at the start of [day]. */
    fun setFor(day: StudyDay) {
        alarmManager.setWindow(
            AlarmManager.RTC,
            clock.startOf(day).toEpochMilliseconds(),
            WINDOW.inWholeMilliseconds,
            alarmIntent()
        )
    }

    /** The last widget is gone. */
    fun cancel() {
        val operation = alarmIntent()
        alarmManager.cancel(operation)
        operation.cancel()
    }

    private fun alarmIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, TodayWidgetRefreshReceiver::class.java).setAction(TodayWidgetRefreshReceiver.ACTION_NEW_DAY),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private companion object {
        /** Android stretches any shorter window to ten minutes on recent versions. */
        val WINDOW = 10.minutes
    }
}
