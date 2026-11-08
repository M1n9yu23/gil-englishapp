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
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class SpeechCacheTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun fileFor_sameTextAnotherLanguageOrRate_isAnotherFile() = runTest {
        val cache = SpeechCache({ tempDir }, StandardTestDispatcher(testScheduler))
        val normal = cache.fileFor(SpeechKey("Hi.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL))

        assertThat(
            cache.fileFor(SpeechKey("Hi.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL))
        ).isEqualTo(normal)
        assertThat(
            cache.fileFor(SpeechKey("Hi.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.SLOW))
        ).isNotEqualTo(normal)
        assertThat(
            cache.fileFor(SpeechKey("Hi.", SpeechLanguage.BRITISH_ENGLISH, SpeechRate.NORMAL))
        ).isNotEqualTo(normal)
        assertThat(normal.name.contains("Hi")).isEqualTo(false)
    }

    @Test
    fun isReady_headerOnly_isFalse() = runTest {
        val cache = SpeechCache({ tempDir }, StandardTestDispatcher(testScheduler))
        val file = File(tempDir, "a.wav").apply { writeBytes(ByteArray(44)) }

        assertThat(cache.isReady(file)).isEqualTo(false)
        assertThat(cache.isReady(File(tempDir, "missing.wav"))).isEqualTo(false)
        file.writeBytes(ByteArray(45))
        assertThat(cache.isReady(file)).isEqualTo(true)
    }

    @Test
    fun trim_overTheLimit_deletesTheOldestFilesNotKept() = runTest {
        val cache = SpeechCache({ tempDir }, StandardTestDispatcher(testScheduler))
        val oldest = file("oldest", modified = 1_000)
        val kept = file("kept", modified = 2_000)
        val older = file("older", modified = 3_000)
        val newest = file("newest", modified = 4_000)

        cache.trim(keep = setOf(kept), maxBytes = 250)

        assertThat(tempDir.listFiles().orEmpty().map { it.name }).containsExactlyInAnyOrder(kept.name, newest.name)
        assertThat(oldest.exists() || older.exists()).isEqualTo(false)
    }

    @Test
    fun trim_underTheLimit_deletesNothing() = runTest {
        val cache = SpeechCache({ tempDir }, StandardTestDispatcher(testScheduler))
        file("a", modified = 1_000)
        file("b", modified = 2_000)

        cache.trim(keep = emptySet(), maxBytes = 200)

        assertThat(tempDir.listFiles().orEmpty().size).isEqualTo(2)
    }

    private fun file(name: String, modified: Long) = File(tempDir, "$name.wav").apply {
        writeBytes(ByteArray(100))
        setLastModified(modified)
    }
}
