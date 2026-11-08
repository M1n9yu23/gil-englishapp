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
package com.gyugle.gil.feature.settings.impl.data.backup

import android.content.Context
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A copy of everything a restore replaces, taken just before the restore starts. */
internal interface BackupSnapshots {
    /** Whether [text] is safely on disk. */
    suspend fun save(text: String): Boolean
}

/**
 * Keeps the latest copy at `files/backup/before-restore.json`, outside the app's backup rules. The
 * restore rolls back from the copy in memory; the file is there for when the app is killed halfway
 * through a restore.
 */
internal class FileBackupSnapshots internal constructor(private val file: () -> File) : BackupSnapshots {

    @Inject
    constructor(@ApplicationContext context: Context) : this({ File(context.filesDir, "backup/before-restore.json") })

    override suspend fun save(text: String): Boolean = withContext(Dispatchers.IO) {
        val target = file()
        val atomic = AtomicFile(target)
        try {
            target.parentFile?.mkdirs()
            val out = atomic.startWrite()
            try {
                out.write(text.encodeToByteArray())
                atomic.finishWrite(out)
                true
            } catch (e: IOException) {
                atomic.failWrite(out)
                throw e
            }
        } catch (_: IOException) {
            false
        }
    }
}
