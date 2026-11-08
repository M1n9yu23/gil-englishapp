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

import android.content.Context
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The cache keeps at most this many bytes of speech, about 20 minutes at 24 kHz. */
internal const val MAX_CACHE_BYTES = 64L * 1024 * 1024

/** A WAV header alone is this long; a file no longer than it holds no sound. */
private const val WAV_HEADER_BYTES = 44L

/** One piece of speech, which is one file in the cache. */
internal data class SpeechKey(val text: String, val language: SpeechLanguage, val rate: SpeechRate)

/**
 * Speech written by the speech engine, kept in the app's cache folder so that a playlist heard again
 * starts at once. The system may clear the folder at any time; a missing file is written again.
 */
@Singleton
internal class SpeechCache(directory: () -> File, private val io: CoroutineDispatcher) {

    @Inject
    constructor(@ApplicationContext context: Context) : this({ File(context.cacheDir, "listening") }, Dispatchers.IO)

    // Finding the cache folder reads the disk, so it waits until the first call on [io].
    private val directory by lazy(directory)

    /** Where [key] is kept. The name is a hash, so no text from the content appears in file names. */
    suspend fun fileFor(key: SpeechKey): File = withContext(io) { File(directory, "${hash(key)}.wav") }

    /** How long [file] plays; zero when its header cannot tell. */
    suspend fun durationOf(file: File): Duration = withContext(io) { wavDuration(file) ?: Duration.ZERO }

    /** Whether [file] holds speech already; marks it as recently used when it does. */
    suspend fun isReady(file: File): Boolean = withContext(io) {
        val ready = file.length() > WAV_HEADER_BYTES
        if (ready) file.setLastModified(System.currentTimeMillis())
        ready
    }

    suspend fun prepare() {
        withContext(io) { directory.mkdirs() }
    }

    /** Deletes the least recently used files beyond [MAX_CACHE_BYTES], never one in [keep]. */
    suspend fun trim(keep: Set<File>, maxBytes: Long = MAX_CACHE_BYTES) = withContext(io) {
        val files = directory.listFiles().orEmpty().filter { it.isFile }
        var total = files.sumOf { it.length() }
        files.filter { it !in keep }.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= maxBytes) return@withContext
            val size = file.length()
            if (file.delete()) total -= size
        }
    }

    private fun hash(key: SpeechKey): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("${key.language.name}|${key.rate.name}|${key.text}".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
