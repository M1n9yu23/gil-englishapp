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

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteFullException
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Test

class DatabaseCallTest {

    @Test
    fun databaseCall_suspendingBlockReturns_success() = runTest {
        val result = databaseCall {
            yield()
            7
        }

        assertThat(result).isEqualTo(Outcome.Success(7))
    }

    @Test
    fun databaseCall_diskFull_diskFullError() = runTest {
        val result = databaseCall { throw SQLiteFullException() }

        assertThat(result).isEqualTo(Outcome.Failure(DataError.Local.DISK_FULL))
    }

    @Test
    fun databaseCall_otherSqliteFailure_unknownError() = runTest {
        val result = databaseCall { throw SQLiteConstraintException() }

        assertThat(result).isEqualTo(Outcome.Failure(DataError.Local.UNKNOWN))
    }

    @Test
    fun databaseCall_bug_rethrows() = runTest {
        assertFailure { databaseCall { error("missing migration") } }.isInstanceOf<IllegalStateException>()
    }

    @Test
    fun databaseCall_cancelled_rethrows() = runTest {
        assertFailure { databaseCall { throw CancellationException("left the screen") } }
            .isInstanceOf<CancellationException>()
    }
}
