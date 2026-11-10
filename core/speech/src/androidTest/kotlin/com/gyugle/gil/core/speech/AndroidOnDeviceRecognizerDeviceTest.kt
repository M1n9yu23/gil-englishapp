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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gyugle.gil.core.domain.Outcome
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Feeds speech made by the device's own speech engine to its on-device recognizer. Skipped where
 * the recognizer has no installed US English model or the engine has no offline voice.
 */
@RunWith(AndroidJUnit4::class)
class AndroidOnDeviceRecognizerDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob())
    private val recognizer = AndroidOnDeviceRecognizer(context)
    private lateinit var speaker: AndroidTextToSpeechSpeaker

    @Before
    fun setUp() = runBlocking<Unit> {
        assumeTrue(
            "No installed on-device en-US recognition model",
            recognizer.availability(SpeechLanguage.AMERICAN_ENGLISH) == RecognizerAvailability.READY
        )
        speaker = AndroidTextToSpeechSpeaker(context, scope)
        val status = withTimeout(10_000) { speaker.status.first { it != SpeakerStatus.Connecting } }
        assumeTrue(
            "No offline en-US voice on this device",
            status is SpeakerStatus.Connected && status.voice(SpeechLanguage.AMERICAN_ENGLISH) == VoiceState.INSTALLED
        )
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun recognize_spokenSentence_returnsItsWords() = runBlocking<Unit> {
        val recording = synthesized("Nice to meet you.")

        val outcome = withTimeout(30_000) { recognizer.recognize(recording, SpeechLanguage.AMERICAN_ENGLISH) }

        assertTrue(
            "Outcome $outcome for ${recording.pcm.size} bytes at ${recording.sampleRateHz} Hz",
            outcome is Outcome.Success
        )
        val heard = (outcome as Outcome.Success).data.lowercase()
        assertTrue("Heard \"$heard\"", "meet" in heard && "you" in heard)
    }

    @Test
    fun recognize_silence_noSpeech() = runBlocking<Unit> {
        val silence = VoiceRecording(ByteArray(SILENCE_BYTES), 16_000)

        val outcome = withTimeout(30_000) { recognizer.recognize(silence, SpeechLanguage.AMERICAN_ENGLISH) }

        assertEquals(Outcome.Failure(RecognitionError.NO_SPEECH), outcome)
    }

    /** The engine's WAV for [text], read back as the mono 16-bit PCM a recording holds. */
    private suspend fun synthesized(text: String): VoiceRecording {
        val file = File(context.cacheDir, "speech-test/recognize.wav")
        val written = withTimeout(10_000) {
            speaker.synthesizeToFile(text, SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file)
        }
        assertEquals(Outcome.Success(Unit), written)
        val wav = ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
        file.delete()
        assertEquals("mono", 1, wav.getShort(CHANNELS_OFFSET).toInt())
        assertEquals("16-bit", 16, wav.getShort(BITS_OFFSET).toInt())
        val sampleRate = wav.getInt(SAMPLE_RATE_OFFSET)
        var chunk = FIRST_CHUNK_OFFSET
        while (chunk + CHUNK_HEADER_BYTES <= wav.limit()) {
            val id = ByteArray(4).also {
                wav.position(chunk)
                wav.get(it)
            }.decodeToString()
            val size = wav.getInt(chunk + 4)
            if (id == "data") {
                val start = chunk + CHUNK_HEADER_BYTES
                val pcm = wav.array().copyOfRange(start, minOf(start + size, wav.limit()))
                return VoiceRecording(pcm, sampleRate)
            }
            chunk += CHUNK_HEADER_BYTES + size
        }
        error("No data chunk in the WAV")
    }

    private companion object {

        const val CHANNELS_OFFSET = 22
        const val SAMPLE_RATE_OFFSET = 24
        const val BITS_OFFSET = 34
        const val FIRST_CHUNK_OFFSET = 12
        const val CHUNK_HEADER_BYTES = 8

        /** Two seconds at 16 kHz. */
        const val SILENCE_BYTES = 64_000
    }
}
