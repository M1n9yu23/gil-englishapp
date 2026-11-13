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
import android.app.Application
import android.content.Context
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.each
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.prop
import com.gyugle.gil.feature.reminder.api.ReminderTime
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ReminderAlarmsTest {
    private val context = RuntimeEnvironment.getApplication()
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val now = Instant.parse("2026-11-07T12:00:00+09:00")
    private val alarms = ReminderAlarms(context, FixedClock(now)) { TimeZone.of("Asia/Seoul") }

    private fun scheduled(): List<ShadowAlarmManager.ScheduledAlarm> = shadowOf(alarmManager).scheduledAlarms

    @Test
    fun schedule_twoTimes_setsOneInexactWakeUpAlarmEach() {
        alarms.schedule(listOf(ReminderTime(8, 0), ReminderTime(20, 30)))

        assertThat(scheduled().map { it.triggerAtMs }).containsExactlyInAnyOrder(
            Instant.parse("2026-11-08T08:00:00+09:00").toEpochMilliseconds(),
            Instant.parse("2026-11-07T20:30:00+09:00").toEpochMilliseconds()
        )
        assertThat(scheduled()).each {
            it.prop(ShadowAlarmManager.ScheduledAlarm::getType).isEqualTo(AlarmManager.RTC_WAKEUP)
            it.prop(ShadowAlarmManager.ScheduledAlarm::getWindowLengthMs)
                .isEqualTo(ReminderAlarms.WINDOW.inWholeMilliseconds)
        }
    }

    @Test
    fun schedule_sameTimesAgain_replacesInsteadOfAdding() {
        val times = listOf(ReminderTime(8, 0), ReminderTime(20, 30))
        alarms.schedule(times)
        alarms.schedule(times)

        assertThat(scheduled().size).isEqualTo(2)
    }

    @Test
    fun schedule_fewerTimes_cancelsTheAlarmOfTheRemovedTime() {
        alarms.schedule(listOf(ReminderTime(8, 0), ReminderTime(20, 30)))
        alarms.schedule(listOf(ReminderTime(21, 0)))

        assertThat(scheduled().map { it.triggerAtMs })
            .containsExactlyInAnyOrder(Instant.parse("2026-11-07T21:00:00+09:00").toEpochMilliseconds())
    }

    @Test
    fun schedule_noTimes_cancelsEveryAlarm() {
        alarms.schedule(listOf(ReminderTime(8, 0), ReminderTime(20, 30)))
        alarms.schedule(emptyList())

        assertThat(scheduled()).isEmpty()
    }

    @Test
    fun schedule_toTheReceiver_withAnExplicitIntent() {
        alarms.schedule(listOf(ReminderTime(8, 0)))

        val intent = shadowOf(scheduled().single().operation).savedIntent
        assertThat(intent.component?.className).isEqualTo(ReminderAlarmReceiver::class.java.name)
    }

    @Test
    fun schedule_carriesTheTimeItIsFor() {
        alarms.schedule(listOf(ReminderTime(20, 0)))

        val alarm = scheduled().single()
        val intent = shadowOf(alarm.operation).savedIntent
        assertThat(intent.getLongExtra(ReminderAlarms.EXTRA_DUE_AT, 0L)).isEqualTo(alarm.triggerAtTime)
    }

    // At 20:33 an alarm for 20:30 may still be on its way; restarting the app must not move it.
    @Test
    fun schedule_keepPendingWithAlarmSet_keepsItsTime() {
        alarms.schedule(listOf(ReminderTime(20, 30)))
        val later = ReminderAlarms(context, FixedClock(Instant.parse("2026-11-07T20:33:00+09:00"))) {
            TimeZone.of("Asia/Seoul")
        }

        later.schedule(listOf(ReminderTime(20, 30)), keepPending = true)

        assertThat(scheduled().map { it.triggerAtMs })
            .containsExactlyInAnyOrder(Instant.parse("2026-11-07T20:30:00+09:00").toEpochMilliseconds())
    }

    @Test
    fun schedule_keepPendingAfterCancel_setsTheMissingAlarm() {
        alarms.schedule(listOf(ReminderTime(20, 30)))
        alarms.schedule(emptyList())

        alarms.schedule(listOf(ReminderTime(20, 30)), keepPending = true)

        assertThat(scheduled().size).isEqualTo(1)
    }

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now(): Instant = instant
    }
}
