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
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The documents the learner picks in the system file picker, so the app needs no storage permission. */
internal interface BackupDocuments {
    /** The text of [uri], or null when it cannot be read or is longer than [MAX_BYTES]. */
    suspend fun read(uri: String): String?

    /** Whether [text] was written to [uri] in full. */
    suspend fun write(uri: String, text: String): Boolean

    companion object {
        /** Far above years of daily study, and small enough to hold in memory. */
        const val MAX_BYTES = 32 * 1024 * 1024
    }
}

internal class ContentResolverBackupDocuments @Inject constructor(
    @param:ApplicationContext private val context: Context
) : BackupDocuments {

    override suspend fun read(uri: String): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri.toUri())?.use { it.readAtMost(BackupDocuments.MAX_BYTES) }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            // The grant from the picker is gone, for example after the provider app was updated.
            null
        }
    }

    override suspend fun write(uri: String, text: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // "wt" so an existing file picked again is replaced rather than partly overwritten.
            val stream = context.contentResolver.openOutputStream(uri.toUri(), "wt") ?: return@withContext false
            stream.use { it.write(text.encodeToByteArray()) }
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun InputStream.readAtMost(limit: Int): String? {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (out.size() + read > limit) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray().decodeToString()
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
    }
}
