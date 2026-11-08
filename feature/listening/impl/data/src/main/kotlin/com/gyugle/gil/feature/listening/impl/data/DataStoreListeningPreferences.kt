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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.gyugle.gil.core.datastore.dataOrEmpty
import com.gyugle.gil.core.datastore.editCatching
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.listening.impl.data.di.ListeningPrefs
import com.gyugle.gil.feature.listening.impl.domain.ListeningPreferences
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.PauseLength
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** A value the app no longer knows reads as the default or as nothing to resume. */
internal class DataStoreListeningPreferences @Inject constructor(
    @param:ListeningPrefs private val store: DataStore<Preferences>
) : ListeningPreferences {

    override fun observePace(): Flow<ListeningPace> = store.dataOrEmpty()
        .map { preferences ->
            val defaults = ListeningPace()
            ListeningPace(
                speed = ListeningSpeed.entries.firstOrNull { it.name == preferences[SPEED] } ?: defaults.speed,
                pause = PauseLength.entries.firstOrNull { it.name == preferences[PAUSE] } ?: defaults.pause
            )
        }
        .distinctUntilChanged()

    override suspend fun setPace(pace: ListeningPace): EmptyOutcome<DataError.Local> = store.editCatching {
        it[SPEED] = pace.speed.name
        it[PAUSE] = pace.pause.name
    }

    override fun observeLastPlayed(): Flow<LastPlayed?> = store.dataOrEmpty()
        .map { preferences ->
            val source = sourceOf(preferences[LAST_SOURCE])
            val track = preferences[LAST_TRACK]
            if (source != null && track != null) LastPlayed(source, track) else null
        }
        .distinctUntilChanged()

    override suspend fun setLastPlayed(lastPlayed: LastPlayed?): EmptyOutcome<DataError.Local> = store.editCatching {
        if (lastPlayed == null) {
            it.remove(LAST_SOURCE)
            it.remove(LAST_TRACK)
        } else {
            it[LAST_SOURCE] = lastPlayed.source.key
            it[LAST_TRACK] = lastPlayed.trackId
        }
    }

    private companion object {
        val SPEED = stringPreferencesKey("speed")
        val PAUSE = stringPreferencesKey("pause")
        val LAST_SOURCE = stringPreferencesKey("last_source")
        val LAST_TRACK = stringPreferencesKey("last_track")
        const val UNIT_PREFIX = "unit:"
    }

    private val PlaylistSource.key: String
        get() = when (this) {
            PlaylistSource.DueToday -> "due"
            PlaylistSource.Recent -> "recent"
            PlaylistSource.Dialogues -> "dialogues"
            is PlaylistSource.CourseUnit -> UNIT_PREFIX + id.value
        }

    private fun sourceOf(key: String?): PlaylistSource? = when {
        key == null -> null

        key == "due" -> PlaylistSource.DueToday

        key == "recent" -> PlaylistSource.Recent

        key == "dialogues" -> PlaylistSource.Dialogues

        key.startsWith(UNIT_PREFIX) && key.length > UNIT_PREFIX.length ->
            PlaylistSource.CourseUnit(UnitId(key.removePrefix(UNIT_PREFIX)))

        else -> null
    }
}
