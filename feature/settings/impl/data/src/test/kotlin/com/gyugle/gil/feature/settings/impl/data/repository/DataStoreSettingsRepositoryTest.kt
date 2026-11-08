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

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.api.ThemeMode
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DataStoreSettingsRepositoryTest {

    @TempDir
    lateinit var tempDir: File

    private val store by lazy { PreferenceDataStoreFactory.create { File(tempDir, "settings_prefs.preferences_pb") } }

    @Test
    fun observeSettings_nothingSaved_emitsDefaults() = runTest {
        assertThat(DataStoreSettingsRepository(store).observeSettings().first()).isEqualTo(AppSettings())
    }

    @Test
    fun setTheme_isObserved() = runTest {
        val repository = DataStoreSettingsRepository(store)

        repository.observeSettings().test {
            assertThat(awaitItem()).isEqualTo(AppSettings())
            assertThat(repository.setTheme(ThemeMode.DARK)).isEqualTo(Outcome.Success(Unit))
            assertThat(awaitItem()).isEqualTo(AppSettings(theme = ThemeMode.DARK))
        }
    }

    // One write per test: on a Windows JVM, DataStore cannot replace a file it has already written.

    @Test
    fun setAccent_changesOnlyTheAccent() = runTest {
        val repository = DataStoreSettingsRepository(store)

        assertThat(repository.setAccent(Accent.BRITISH)).isEqualTo(Outcome.Success(Unit))

        assertThat(repository.observeSettings().first()).isEqualTo(AppSettings(accent = Accent.BRITISH))
    }

    @Test
    fun setSpeed_changesOnlyTheSpeed() = runTest {
        val repository = DataStoreSettingsRepository(store)

        repository.setSpeed(SpeechSpeed.SLOW)

        assertThat(repository.observeSettings().first()).isEqualTo(AppSettings(speed = SpeechSpeed.SLOW))
    }

    @Test
    fun setShowHangulAid_changesOnlyTheAid() = runTest {
        val repository = DataStoreSettingsRepository(store)

        repository.setShowHangulAid(false)

        assertThat(repository.observeSettings().first()).isEqualTo(AppSettings(showHangulAid = false))
    }

    @Test
    fun setAll_writesEverySettingAtOnce() = runTest {
        val repository = DataStoreSettingsRepository(store)
        val settings = AppSettings(ThemeMode.LIGHT, Accent.BRITISH, SpeechSpeed.SLOW, showHangulAid = false)

        repository.setAll(settings)

        assertThat(repository.observeSettings().first()).isEqualTo(settings)
    }

    @Test
    fun observeSettings_savedValueTheAppDoesNotKnow_readsAsTheDefault() = runTest {
        store.edit {
            it[stringPreferencesKey("theme")] = "sepia"
            it[stringPreferencesKey("accent")] = "british"
        }

        assertThat(DataStoreSettingsRepository(store).observeSettings().first())
            .isEqualTo(AppSettings(accent = Accent.BRITISH))
    }
}
