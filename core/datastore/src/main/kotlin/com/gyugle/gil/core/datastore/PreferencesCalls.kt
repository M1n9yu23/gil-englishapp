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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import java.io.IOException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

/**
 * The stored preferences, or empty preferences when the file cannot be read, so the screen shows
 * defaults instead of crashing. A damaged file is already replaced by the factory; this covers
 * other read errors. The flow ends after such an error, so later changes do not arrive.
 */
fun DataStore<Preferences>.dataOrEmpty(): Flow<Preferences> = data.catch { cause ->
    if (cause is IOException) emit(emptyPreferences()) else throw cause
}

/** Writes with [transform] and reports a failed write as [DataError.Local.UNKNOWN]. */
suspend fun DataStore<Preferences>.editCatching(
    transform: suspend (MutablePreferences) -> Unit
): EmptyOutcome<DataError.Local> = try {
    edit(transform)
    Outcome.Success(Unit)
} catch (_: IOException) {
    currentCoroutineContext().ensureActive()
    Outcome.Failure(DataError.Local.UNKNOWN)
}
