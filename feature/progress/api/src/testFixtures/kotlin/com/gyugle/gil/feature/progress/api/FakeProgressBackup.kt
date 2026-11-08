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
package com.gyugle.gil.feature.progress.api

import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome

/**
 * Keeps the record as the section text itself. A section is valid when it is a key of [previews],
 * so a test decides what a file holds; [EMPTY] is always valid. [exportError], [restoreError] and
 * [clearError], when set, fail those calls without changing [record].
 */
class FakeProgressBackup(var record: String = EMPTY) : ProgressBackup {
    val previews = mutableMapOf(EMPTY to ProgressBackupPreview(0, 0, null))
    val restored = mutableListOf<String>()
    var exportError: DataError.Local? = null
    var restoreError: BackupError? = null
    var clearError: DataError.Local? = null

    override suspend fun export(): Outcome<String, DataError.Local> =
        exportError?.let { Outcome.Failure(it) } ?: Outcome.Success(record)

    override suspend fun preview(section: String): Outcome<ProgressBackupPreview, BackupError> =
        previews[section]?.let { Outcome.Success(it) } ?: Outcome.Failure(BackupError.Format.MALFORMED)

    override suspend fun restore(section: String): EmptyOutcome<BackupError> {
        if (section !in previews) return Outcome.Failure(BackupError.Format.MALFORMED)
        restoreError?.let { return Outcome.Failure(it) }
        restored += section
        record = section
        return Outcome.Success(Unit)
    }

    override suspend fun clear(): EmptyOutcome<DataError.Local> {
        clearError?.let { return Outcome.Failure(it) }
        record = EMPTY
        return Outcome.Success(Unit)
    }

    companion object {
        const val EMPTY = """{"version":1}"""
    }
}
