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
package com.gyugle.gil.core.speech

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.gyugle.gil.core.domain.ApplicationScope
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * [Speaker] over the platform `TextToSpeech`.
 *
 * Files are written by a second engine instance. A shared one would let `QUEUE_FLUSH` from
 * [speak] drop a file that is still being written.
 *
 * Creating it binds the system's speech engine, so inject it only where something is spoken.
 */
@Singleton
class AndroidTextToSpeechSpeaker internal constructor(
    context: Context,
    scope: CoroutineScope,
    private val io: CoroutineDispatcher
) : Speaker {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @ApplicationScope scope: CoroutineScope
    ) : this(context, scope, Dispatchers.IO)

    private val playback = TextToSpeechConnection(context, scope, io).apply { connect() }
    private val files = lazy { TextToSpeechConnection(context, scope, io).apply { connect() } }
    private val fileLock = Mutex()

    override val status: StateFlow<SpeakerStatus> = playback.status

    override fun refresh() {
        playback.refresh()
        if (files.isInitialized()) files.value.refresh()
    }

    override suspend fun speak(text: String, language: SpeechLanguage, rate: SpeechRate): EmptyOutcome<SpeechError> =
        playback.utter(text, language, rate, TextToSpeechConnection.Kind.SPEECH) { tts, id ->
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        }

    override suspend fun synthesizeToFile(
        text: String,
        language: SpeechLanguage,
        rate: SpeechRate,
        file: File
    ): EmptyOutcome<SpeechError> = fileLock.withLock {
        // Written beside the target and moved over it at the end, so a failure keeps the old file
        // and nobody reads a half-written one.
        val part = File(file.parentFile, "${file.name}.part")
        var moved = false
        try {
            withContext(io) { file.parentFile?.mkdirs() }
            val outcome = files.value.utter(text, language, rate, TextToSpeechConnection.Kind.FILE) { tts, id ->
                tts.synthesizeToFile(text, Bundle(), part, id)
            }
            if (outcome !is Outcome.Success) return@withLock outcome
            moved = withContext(io) { moveOver(part, file) }
            if (moved) outcome else Outcome.Failure(SpeechError.FAILED)
        } finally {
            if (!moved) withContext(NonCancellable + io) { part.delete() }
        }
    }

    // An atomic move is a rename, which replaces the target; other copy options are ignored with it.
    private fun moveOver(source: File, target: File): Boolean = try {
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE)
        true
    } catch (_: IOException) {
        Timber.w("Could not move a written speech file into place")
        false
    }
}
