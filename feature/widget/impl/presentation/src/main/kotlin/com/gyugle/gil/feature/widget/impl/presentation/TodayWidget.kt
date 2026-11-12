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

import android.annotation.SuppressLint
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.currentState
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.widget.impl.presentation.di.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * Today's reviews and the day's sentence on the home screen.
 *
 * It follows the record while Glance keeps the widget's session open, and the app updates it when
 * the record changes ([WidgetStartup]). An update to a session that is still open does not run
 * [provideGlance] again, so when the day, the clock or the language changes, [refreshAll] also
 * counts up a value in the widget's state, and the session reads the day and the language again.
 */
class TodayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(ROW_SIZE, FULL_SIZE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val graph = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
        fun states(today: StudyDay): Flow<TodayWidgetState> = graph.reader().observe(today)
            .map { it.toState() }
            .catch { e ->
                Timber.e(e, "Could not read the study record for the widget")
                emit(TodayWidgetState.Unavailable)
            }

        // Read before composing, so the first frame already has the content.
        val first = states(graph.clock().today()).first()
        provideContent {
            val refreshes = currentState(REFRESHES)
            val today = remember(refreshes) { graph.clock().today() }
            val localized = remember(refreshes) { context.inAppLanguage() }
            val state by produceState(first, today) {
                // Also after the clock went back: the pending alarm may be a day too late.
                graph.dayRefreshAlarm().setFor(today + 1)
                states(today).collect { value = it }
            }
            CompositionLocalProvider(LocalContext provides localized) {
                TodayWidgetLayout(state)
            }
        }
    }

    companion object {
        private val REFRESHES = longPreferencesKey("refreshes")

        /** Every widget on the home screen reads the day and the language again. */
        suspend fun refreshAll(context: Context) {
            GlanceAppWidgetManager(context).getGlanceIds(TodayWidget::class.java).forEach { id ->
                updateAppWidgetState(context, id) { it[REFRESHES] = (it[REFRESHES] ?: 0) + 1 }
            }
            TodayWidget().updateAll(context)
        }
    }
}

/**
 * This context in the app's language as it is now. The context Glance gives keeps the language the
 * process started with when the learner changes the app's language in settings.
 *
 * Lint reads setLocales as switching to a language the bundle may not have installed. These are the
 * locales the system already applied to the app, so their resources are there.
 */
@SuppressLint("AppBundleLocaleChanges")
private fun Context.inAppLanguage(): Context {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return this
    val localeManager = getSystemService(LocaleManager::class.java) ?: return this
    val locales = localeManager.applicationLocales.takeUnless { it.isEmpty } ?: localeManager.systemLocales
    val configuration = Configuration(resources.configuration).apply { setLocales(locales) }
    return createConfigurationContext(configuration)
}
