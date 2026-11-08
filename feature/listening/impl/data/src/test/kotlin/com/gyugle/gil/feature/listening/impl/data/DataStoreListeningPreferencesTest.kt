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
package com.gyugle.gil.feature.listening.impl.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.PauseLength
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

// One write per test: on a Windows JVM, DataStore cannot replace a file it has already written.
class DataStoreListeningPreferencesTest {

    @TempDir
    lateinit var tempDir: File

    private val store by lazy { PreferenceDataStoreFactory.create { File(tempDir, "listening_prefs.preferences_pb") } }
    private val preferences by lazy { DataStoreListeningPreferences(store) }

    @Test
    fun nothingSaved_defaultPaceAndNothingToResume() = runTest {
        assertThat(preferences.observePace().first()).isEqualTo(ListeningPace())
        assertThat(preferences.observeLastPlayed().first()).isNull()
    }

    @Test
    fun setPace_isObserved() = runTest {
        val pace = ListeningPace(ListeningSpeed.SLOW, PauseLength.LONG)

        assertThat(preferences.setPace(pace)).isEqualTo(Outcome.Success(Unit))

        assertThat(preferences.observePace().first()).isEqualTo(pace)
    }

    @Test
    fun setLastPlayed_unit_comesBackWithItsId() = runTest {
        val last = LastPlayed(PlaylistSource.CourseUnit(UnitId("unit.a1-02")), "sense.name.n.what-called")

        preferences.setLastPlayed(last)

        assertThat(preferences.observeLastPlayed().first()).isEqualTo(last)
    }

    @Test
    fun setLastPlayed_dialogues_comesBack() = runTest {
        val last = LastPlayed(PlaylistSource.Dialogues, "lesson.a1-01.2#0")

        preferences.setLastPlayed(last)

        assertThat(preferences.observeLastPlayed().first()).isEqualTo(last)
    }

    @Test
    fun unknownValues_readAsDefaultsAndNothingToResume() = runTest {
        store.edit {
            it[stringPreferencesKey("speed")] = "FAST"
            it[stringPreferencesKey("pause")] = "LONG"
            it[stringPreferencesKey("last_source")] = "unit:"
            it[stringPreferencesKey("last_track")] = "pattern.hi"
        }

        assertThat(preferences.observePace().first()).isEqualTo(ListeningPace(pause = PauseLength.LONG))
        assertThat(preferences.observeLastPlayed().first()).isNull()
    }
}
