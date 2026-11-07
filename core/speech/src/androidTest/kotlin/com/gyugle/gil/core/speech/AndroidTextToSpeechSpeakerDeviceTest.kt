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

/** Runs against the device's real speech engine; skipped where it has no offline US English voice. */
@RunWith(AndroidJUnit4::class)
class AndroidTextToSpeechSpeakerDeviceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob())
    private lateinit var speaker: AndroidTextToSpeechSpeaker

    @Before
    fun setUp() = runBlocking<Unit> {
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
    fun speak_americanEnglish_finishes() = runBlocking<Unit> {
        val outcome = withTimeout(10_000) { speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH) }

        assertEquals(Outcome.Success(Unit), outcome)
    }

    @Test
    fun synthesizeToFile_americanEnglish_writesWav() = runBlocking<Unit> {
        val file = File(context.cacheDir, "speech-test/hello.wav")

        val outcome = withTimeout(10_000) {
            speaker.synthesizeToFile("Hello.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file)
        }

        assertEquals(Outcome.Success(Unit), outcome)
        assertEquals("RIFF", file.readBytes().copyOf(4).decodeToString())
        assertTrue(file.length() > WAV_HEADER_BYTES)
        file.delete()
    }

    private companion object {
        const val WAV_HEADER_BYTES = 44
    }
}
