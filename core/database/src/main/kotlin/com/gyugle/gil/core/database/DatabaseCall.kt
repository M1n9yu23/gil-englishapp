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
package com.gyugle.gil.core.database

import android.database.SQLException
import android.database.sqlite.SQLiteFullException
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome

/**
 * Runs a database write or read and turns SQLite failures into [DataError.Local].
 *
 * Only SQLite exceptions are caught. Anything else, such as a missing migration, is a bug and
 * should crash. Cancellation passes through because it is not an [SQLException]. The block is
 * inlined, so it may call suspending DAO functions. This assumes Room's default framework
 * SQLite; a custom SQLiteDriver throws androidx.sqlite.SQLiteException, which is not caught here.
 */
inline fun <T> databaseCall(block: () -> T): Outcome<T, DataError.Local> = try {
    Outcome.Success(block())
} catch (_: SQLiteFullException) {
    Outcome.Failure(DataError.Local.DISK_FULL)
} catch (_: SQLException) {
    Outcome.Failure(DataError.Local.UNKNOWN)
}
