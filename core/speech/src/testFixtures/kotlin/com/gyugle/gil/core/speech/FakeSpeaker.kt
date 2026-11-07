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

import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [Speaker] that records what it was asked to say. Every voice is installed until a test
 * changes [status]. A successful [synthesizeToFile] leaves an empty file, so the file exists.
 */
class FakeSpeaker : Speaker {
    data class Utterance(val text: String, val language: SpeechLanguage, val rate: SpeechRate)

    override val status: MutableStateFlow<SpeakerStatus> = MutableStateFlow(
        SpeakerStatus.Connected(SpeechLanguage.entries.associateWith { VoiceState.INSTALLED })
    )

    val spoken = mutableListOf<Utterance>()
    val synthesized = mutableListOf<Pair<Utterance, File>>()
    var refreshCount = 0
        private set

    /** Returned by [speak] and [synthesizeToFile] instead of success. */
    var error: SpeechError? = null

    /** When set, [speak] suspends until the test completes it, to observe the speaking state. */
    var speaking: CompletableDeferred<Unit>? = null

    override fun refresh() {
        refreshCount++
    }

    override suspend fun speak(text: String, language: SpeechLanguage, rate: SpeechRate): EmptyOutcome<SpeechError> {
        spoken += Utterance(text, language, rate)
        speaking?.await()
        return outcome()
    }

    override suspend fun synthesizeToFile(
        text: String,
        language: SpeechLanguage,
        rate: SpeechRate,
        file: File
    ): EmptyOutcome<SpeechError> {
        synthesized += Utterance(text, language, rate) to file
        if (error == null) {
            file.parentFile?.mkdirs()
            file.writeBytes(ByteArray(0))
        }
        return outcome()
    }

    private fun outcome(): EmptyOutcome<SpeechError> = error?.let { Outcome.Failure(it) } ?: Outcome.Success(Unit)
}
