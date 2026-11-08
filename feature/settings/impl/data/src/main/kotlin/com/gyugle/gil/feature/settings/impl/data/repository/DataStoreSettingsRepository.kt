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
package com.gyugle.gil.feature.settings.impl.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gyugle.gil.core.datastore.dataOrEmpty
import com.gyugle.gil.core.datastore.editCatching
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.api.ThemeMode
import com.gyugle.gil.feature.settings.impl.data.di.SettingsPreferences
import com.gyugle.gil.feature.settings.impl.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** A value the app no longer knows reads as the default, so an old or odd file never stops the app. */
@Singleton
internal class DataStoreSettingsRepository @Inject constructor(
    @param:SettingsPreferences private val store: DataStore<Preferences>
) : SettingsRepository {

    override fun observeSettings(): Flow<AppSettings> = store.dataOrEmpty()
        .map { preferences ->
            val defaults = AppSettings()
            AppSettings(
                theme = themeOf(preferences[THEME]) ?: defaults.theme,
                accent = accentOf(preferences[ACCENT]) ?: defaults.accent,
                speed = speedOf(preferences[SPEED]) ?: defaults.speed,
                showHangulAid = preferences[HANGUL_AID] ?: defaults.showHangulAid
            )
        }
        .distinctUntilChanged()

    override suspend fun setTheme(theme: ThemeMode): EmptyOutcome<DataError.Local> =
        store.editCatching { it[THEME] = theme.key }

    override suspend fun setAccent(accent: Accent): EmptyOutcome<DataError.Local> =
        store.editCatching { it[ACCENT] = accent.key }

    override suspend fun setSpeed(speed: SpeechSpeed): EmptyOutcome<DataError.Local> =
        store.editCatching { it[SPEED] = speed.key }

    override suspend fun setShowHangulAid(show: Boolean): EmptyOutcome<DataError.Local> =
        store.editCatching { it[HANGUL_AID] = show }

    override suspend fun setAll(settings: AppSettings): EmptyOutcome<DataError.Local> = store.editCatching {
        it[THEME] = settings.theme.key
        it[ACCENT] = settings.accent.key
        it[SPEED] = settings.speed.key
        it[HANGUL_AID] = settings.showHangulAid
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ACCENT = stringPreferencesKey("accent")
        val SPEED = stringPreferencesKey("speech_speed")
        val HANGUL_AID = booleanPreferencesKey("show_hangul_aid")
    }
}
