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
package com.gyugle.gil

import android.app.Application
import android.os.StrictMode
import com.gyugle.gil.feature.reminder.impl.data.ReminderStartup
import com.gyugle.gil.feature.widget.impl.presentation.WidgetStartup
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import timber.log.Timber

@HiltAndroidApp
class GilApplication : Application() {
    @Inject lateinit var reminderStartup: ReminderStartup

    @Inject lateinit var widgetStartup: WidgetStartup

    override fun onCreate() {
        super.onCreate()
        // Release builds plant no tree, so nothing is logged there.
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
            enableStrictMode()
        }
        reminderStartup.start()
        widgetStartup.start()
    }

    // Violations are logged, not fatal, so a debug run keeps going; read them in logcat.
    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().detectAll().penaltyLog().build())
    }
}
