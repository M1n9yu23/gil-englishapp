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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.gyugle.gil.feature.widget.impl.presentation.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import timber.log.Timber

/** The launcher's way in: it asks this receiver to draw, resize and remove the widget. */
class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = TodayWidget()

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).dayRefreshAlarm().cancel()
    }
}

/**
 * A new study day began, or the clock or time zone changed: the widget shows another day. Or the
 * language changed, the phone's or the app's own: the widget's words change with it.
 */
internal class TodayWidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REFRESH_ACTIONS) return
        val scope = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).applicationScope()
        val pending = goAsync()
        scope.launch {
            try {
                TodayWidget.refreshAll(context)
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Timber.w("Could not update the widget: %s", e::class.simpleName)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_NEW_DAY = "com.gyugle.gil.feature.widget.NEW_DAY"

        private val REFRESH_ACTIONS = setOf(
            ACTION_NEW_DAY,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_LOCALE_CHANGED
        )
    }
}
