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
 * The learning record's section of a backup file. The section is a JSON object that carries its
 * own format version, so the file around it does not need to know what is inside.
 */
interface ProgressBackup {
    /** Writes only what [restore] accepts, so a file the app saved can always be restored. */
    suspend fun export(): Outcome<String, DataError.Local>

    /** Reads [section] without writing anything, for the summary shown before a restore. */
    suspend fun preview(section: String): Outcome<ProgressBackupPreview, BackupError>

    /** Replaces the whole learning record with [section]. On any failure the record stays as it was. */
    suspend fun restore(section: String): EmptyOutcome<BackupError>
}
