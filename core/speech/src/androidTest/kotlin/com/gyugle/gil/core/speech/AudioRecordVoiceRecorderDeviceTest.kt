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

import android.Manifest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.gyugle.gil.core.domain.Outcome
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Records from the device's real microphone. On an emulator without a host microphone it records silence. */
@RunWith(AndroidJUnit4::class)
class AudioRecordVoiceRecorderDeviceTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Before
    fun setUp() {
        instrumentation.uiAutomation.grantRuntimePermission(context.packageName, Manifest.permission.RECORD_AUDIO)
    }

    @Test
    fun record_halfSecond_returnsHalfSecondOf16KhzAudio() = runBlocking<Unit> {
        val outcome = withTimeout(5_000) { AudioRecordVoiceRecorder(context).record(500.milliseconds) }

        val recording = (outcome as Outcome.Success).data
        assertEquals(16_000, recording.sampleRateHz)
        assertEquals(500.milliseconds, recording.duration)
    }
}
