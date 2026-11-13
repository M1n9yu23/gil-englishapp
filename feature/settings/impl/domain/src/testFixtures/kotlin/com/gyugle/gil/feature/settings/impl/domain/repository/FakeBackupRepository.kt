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

import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.settings.impl.domain.model.BackupPreview
import kotlinx.coroutines.CompletableDeferred

/**
 * Records the documents it was given and answers with [exportResult], [previewResult] or
 * [restoreResult]. When [pending] is set, every call waits for it first, so a test can see the busy state.
 */
class FakeBackupRepository : BackupRepository {
    var exportResult: EmptyOutcome<BackupError> = Outcome.Success(Unit)
    var previewResult: Outcome<BackupPreview, BackupError> = Outcome.Failure(BackupError.FileUnavailable)
    var restoreResult: EmptyOutcome<BackupError> = Outcome.Success(Unit)
    var pending: CompletableDeferred<Unit>? = null
    val exported = mutableListOf<String>()
    val previewed = mutableListOf<String>()
    val restored = mutableListOf<String>()

    override suspend fun export(uri: String): EmptyOutcome<BackupError> {
        exported += uri
        pending?.await()
        return exportResult
    }

    override suspend fun preview(uri: String): Outcome<BackupPreview, BackupError> {
        previewed += uri
        pending?.await()
        return previewResult
    }

    override suspend fun restore(uri: String): EmptyOutcome<BackupError> {
        restored += uri
        pending?.await()
        return restoreResult
    }

    /** Whether a copy from an earlier restore is still kept; [deleteSnapshot] clears it. */
    var hasSnapshot = true

    override suspend fun deleteSnapshot() {
        hasSnapshot = false
    }
}
