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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.time.Duration.Companion.milliseconds
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class WavDurationTest {

    @TempDir
    lateinit var tempDir: File

    /** 24 kHz, 16-bit mono, as the speech engine writes it: 48,000 bytes a second. */
    private val byteRate = 48_000

    @Test
    fun wavDuration_canonicalHeader_isTheDataLengthOverTheByteRate() {
        val file = wav(dataBytes = 72_000, declaredSize = 72_000)

        assertThat(wavDuration(file)).isEqualTo(1_500.milliseconds)
    }

    @Test
    fun wavDuration_extraChunkBeforeTheData_isSkipped() {
        val file = wav(dataBytes = 24_000, declaredSize = 24_000, extraChunk = 5)

        assertThat(wavDuration(file)).isEqualTo(500.milliseconds)
    }

    @Test
    fun wavDuration_dataSizeLeftUnset_usesTheRestOfTheFile() {
        val file = wav(dataBytes = 48_000, declaredSize = 0)

        assertThat(wavDuration(file)).isEqualTo(1_000.milliseconds)
    }

    @Test
    fun wavDuration_notAWav_isNull() {
        assertThat(wavDuration(File(tempDir, "empty.wav").apply { writeBytes(ByteArray(0)) })).isNull()
        assertThat(wavDuration(File(tempDir, "text.wav").apply { writeText("Hello, this is not sound.") })).isNull()
        assertThat(wavDuration(File(tempDir, "missing.wav"))).isNull()
    }

    private fun wav(dataBytes: Int, declaredSize: Int, extraChunk: Int? = null): File {
        val extra = extraChunk?.let { 8 + it + (it and 1) } ?: 0
        val buffer = ByteBuffer.allocate(44 + extra + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray()).putInt(36 + extra + dataBytes).put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray()).putInt(16)
            .putShort(1).putShort(1).putInt(24_000).putInt(byteRate).putShort(2).putShort(16)
        extraChunk?.let { size ->
            buffer.put("LIST".toByteArray()).putInt(size).put(ByteArray(size + (size and 1)))
        }
        buffer.put("data".toByteArray()).putInt(declaredSize).put(ByteArray(dataBytes))
        return File(tempDir, "speech.wav").apply { writeBytes(buffer.array()) }
    }
}
