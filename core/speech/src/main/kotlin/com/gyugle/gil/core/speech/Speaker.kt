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
import java.io.File
import kotlinx.coroutines.flow.StateFlow

/**
 * Text to speech with offline voices only.
 *
 * Only voices that need no network are used, so the text never leaves the device even when the
 * engine has a better online voice.
 */
interface Speaker {
    val status: StateFlow<SpeakerStatus>

    /**
     * Reads the installed voices again, and reconnects when there was no engine. Call it when the
     * learner comes back from installing voice data or a speech engine.
     */
    fun refresh()

    /**
     * Speaks [text] and suspends until it has been spoken. Cancelling the call stops the speech.
     *
     * A later call interrupts this one, which then returns success: the newer text is what the
     * learner asked to hear.
     */
    suspend fun speak(
        text: String,
        language: SpeechLanguage,
        rate: SpeechRate = SpeechRate.NORMAL
    ): EmptyOutcome<SpeechError>

    /**
     * Writes [text] as a WAV file to [file], replacing it. Calls run one at a time and do not
     * interrupt [speak]. A failed or cancelled call leaves [file] as it was.
     *
     * Listening mode needs files because speech from [speak] plays in the engine's process, where
     * the app cannot handle audio focus, headphone unplugging or a playback position.
     */
    suspend fun synthesizeToFile(
        text: String,
        language: SpeechLanguage,
        rate: SpeechRate,
        file: File
    ): EmptyOutcome<SpeechError>
}
