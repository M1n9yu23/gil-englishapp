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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.gyugle.gil.feature.home.api.TODAY_DEEP_LINK
import com.gyugle.gil.feature.reminder.impl.domain.model.ReminderMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Posts the study reminder on its own channel. Tapping it opens the Today tab. */
internal class ReminderNotifier @Inject constructor(@param:ApplicationContext private val context: Context) {

    private val notificationManager = NotificationManagerCompat.from(context)

    /** Safe to call at every start: an existing channel keeps the learner's settings. */
    fun createChannel() {
        notificationManager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.reminder_channel_name))
                .setDescription(context.getString(R.string.reminder_channel_description))
                .build()
        )
    }

    /** Does nothing without the notification permission or with the app's notifications turned off. */
    fun show(message: ReminderMessage) {
        if (!notificationManager.areNotificationsEnabled()) return
        // In this method, not a helper: lint only sees a permission check made where notify is called.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_reminder)
            .setContentTitle(context.getString(R.string.reminder_notification_title))
            .setContentText(message.text())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openToday())
            .setAutoCancel(true)
            .build()
        // One id for both daily reminders: the second replaces the first instead of piling up.
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun ReminderMessage.text(): String = when (this) {
        is ReminderMessage.Reviews ->
            context.resources.getQuantityString(R.plurals.reminder_notification_reviews, count, count, minutes)

        ReminderMessage.NextLesson -> context.getString(R.string.reminder_notification_next_lesson)
    }

    /**
     * The launch activity with the Today link. With the app already open, the running activity gets
     * the link instead of a second copy starting above it.
     */
    private fun openToday(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        intent.data = TODAY_DEEP_LINK.toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "study_reminders"
        const val NOTIFICATION_ID = 1
    }
}
