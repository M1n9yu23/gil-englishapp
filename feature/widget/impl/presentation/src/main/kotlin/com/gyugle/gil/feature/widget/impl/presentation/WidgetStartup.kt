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

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.gyugle.gil.core.domain.ApplicationScope
import com.gyugle.gil.feature.progress.api.ProgressObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

/** Called once from `Application.onCreate`. */
class WidgetStartup @Inject internal constructor(
    @param:ApplicationContext private val context: Context,
    private val progress: ProgressObserver,
    @param:ApplicationScope private val scope: CoroutineScope
) {
    /**
     * Updates the widget at start and whenever the record changes while the app runs, such as after
     * each card reviewed. With no widget on the home screen, an update does nothing.
     */
    fun start() {
        scope.launch {
            progress.observeCards()
                .catch { e -> Timber.w("Stopped updating the widget: %s", e::class.simpleName) }
                .collectLatest { updateWidgets() }
        }
    }

    private suspend fun updateWidgets() {
        try {
            TodayWidget().updateAll(context)
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Timber.w("Could not update the widget: %s", e::class.simpleName)
        }
    }
}
