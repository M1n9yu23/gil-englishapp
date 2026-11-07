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
import android.app.Application
import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.single
import com.gyugle.gil.feature.reminder.impl.domain.model.ReminderMessage
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class, qualifiers = "ko")
class ReminderNotifierTest {
    private val context = RuntimeEnvironment.getApplication()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val notifier = ReminderNotifier(context)

    @Before
    fun setUp() {
        notifier.createChannel()
    }

    private fun posted(): List<Notification> = shadowOf(notificationManager).allNotifications

    @Test
    fun createChannel_createsTheReminderChannelWithItsName() {
        val channel = notificationManager.getNotificationChannel(ReminderNotifier.CHANNEL_ID)

        assertThat(channel).isNotNull()
        assertThat(channel.name.toString()).isEqualTo("공부 알림")
    }

    @Test
    fun show_reviewsDue_namesCountAndMinutes() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(ReminderMessage.Reviews(count = 12, minutes = 6))

        assertThat(posted()).single().transform { it.extras.getString(Notification.EXTRA_TITLE) to it.text() }
            .isEqualTo("오늘의 길" to "복습 12개, 약 6분")
    }

    @Test
    @Config(qualifiers = "en")
    fun show_oneReviewInEnglish_usesTheSingular() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(ReminderMessage.Reviews(count = 1, minutes = 1))

        assertThat(posted()).single().transform { it.text() }.isEqualTo("1 review, about 1 min")
    }

    @Test
    fun show_twice_replacesTheFirst() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(ReminderMessage.NextLesson)
        notifier.show(ReminderMessage.Reviews(count = 3, minutes = 2))

        assertThat(posted()).single().transform { it.text() }.isEqualTo("복습 3개, 약 2분")
    }

    @Test
    fun show_withoutPermission_postsNothing() {
        shadowOf(context).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        notifier.show(ReminderMessage.NextLesson)

        assertThat(posted()).isEmpty()
    }

    @Test
    fun show_notificationsTurnedOff_postsNothing() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        shadowOf(notificationManager).setNotificationsEnabled(false)

        notifier.show(ReminderMessage.NextLesson)

        assertThat(posted()).isEmpty()
    }

    @Test
    fun show_tapped_opensTheLaunchActivityWithTheTodayLink() {
        shadowOf(context).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
        // The app module declares the real launch activity; a library test registers a stand-in.
        val launcher = ComponentName(context, "com.gyugle.gil.MainActivity")
        shadowOf(context.packageManager).apply {
            addActivityIfNotPresent(launcher)
            addIntentFilterForActivity(
                launcher,
                IntentFilter(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            )
        }

        notifier.show(ReminderMessage.NextLesson)

        val intent = shadowOf(posted().single().contentIntent).savedIntent
        assertThat(intent.component).isEqualTo(launcher)
        assertThat(intent.dataString).isEqualTo("gil://today")
    }

    private fun Notification.text(): String? = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
}
