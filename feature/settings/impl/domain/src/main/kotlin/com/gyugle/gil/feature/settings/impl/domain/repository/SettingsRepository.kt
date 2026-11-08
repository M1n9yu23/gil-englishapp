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
package com.gyugle.gil.feature.settings.impl.domain.repository

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.AppSettingsObserver
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.api.ThemeMode

/** The settings this feature owns, read by others through [AppSettingsObserver]. */
interface SettingsRepository : AppSettingsObserver {
    suspend fun setTheme(theme: ThemeMode): EmptyOutcome<DataError.Local>

    suspend fun setAccent(accent: Accent): EmptyOutcome<DataError.Local>

    suspend fun setSpeed(speed: SpeechSpeed): EmptyOutcome<DataError.Local>

    suspend fun setShowHangulAid(show: Boolean): EmptyOutcome<DataError.Local>

    /** Writes every setting in one go, as a restored backup does. */
    suspend fun setAll(settings: AppSettings): EmptyOutcome<DataError.Local>
}
