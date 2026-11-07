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
package com.gyugle.gil.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates one preferences DataStore per feature, each in its own file.
 *
 * Files land at `files/datastore/<name>.preferences_pb`, the same place
 * `Context.preferencesDataStoreFile` uses. The backup rules in the app list these paths by name,
 * so a renamed file silently drops out of backups.
 */
@Singleton
class DataStoreFactory internal constructor(private val directory: () -> File) {

    @Inject
    constructor(@ApplicationContext context: Context) : this({ File(context.applicationContext.filesDir, "datastore") })

    private val registered = mutableSetOf<String>()

    /**
     * Returns the DataStore for [name], such as `goal_prefs`. Call it once per name for the life of
     * the process, from the owning feature's singleton binding.
     */
    fun create(name: String): DataStore<Preferences> {
        // DataStore itself only fails at the first read when two instances share a file.
        synchronized(registered) {
            check(registered.add(name)) { "DataStore file '$name' is already registered" }
        }
        // Without a handler a damaged file fails every later write too, because edit reads first.
        return PreferenceDataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
        ) { File(directory(), "$name.preferences_pb") }
    }
}
