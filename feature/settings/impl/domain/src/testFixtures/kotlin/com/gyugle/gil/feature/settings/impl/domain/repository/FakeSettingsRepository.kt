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
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.api.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Keeps the settings in [settings]. [error], when set, fails every write without changing them. */
class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    val settings = MutableStateFlow(initial)
    var error: DataError.Local? = null

    override fun observeSettings(): Flow<AppSettings> = settings

    override suspend fun setTheme(theme: ThemeMode) = write { it.copy(theme = theme) }

    override suspend fun setAccent(accent: Accent) = write { it.copy(accent = accent) }

    override suspend fun setSpeed(speed: SpeechSpeed) = write { it.copy(speed = speed) }

    override suspend fun setShowHangulAid(show: Boolean) = write { it.copy(showHangulAid = show) }

    override suspend fun setAll(settings: AppSettings) = write { settings }

    private fun write(transform: (AppSettings) -> AppSettings): EmptyOutcome<DataError.Local> {
        error?.let { return Outcome.Failure(it) }
        settings.update(transform)
        return Outcome.Success(Unit)
    }
}
