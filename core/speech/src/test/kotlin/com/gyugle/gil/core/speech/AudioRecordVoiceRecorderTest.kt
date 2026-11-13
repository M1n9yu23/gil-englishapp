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
import android.app.Application
import android.media.AudioRecord
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.Outcome
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAudioRecord
import timber.log.Timber

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AudioRecordVoiceRecorderTest {

    private val context = RuntimeEnvironment.getApplication()

    /** Fills every read with an increasing byte pattern and counts the reads. */
    private class CountingSource(private val onRead: (Int) -> Int = { it }) : ShadowAudioRecord.AudioRecordSource {
        var reads = 0
        private var next: Byte = 0

        override fun readInByteArray(
            audioData: ByteArray,
            offsetInBytes: Int,
            sizeInBytes: Int,
            isBlocking: Boolean
        ): Int {
            reads++
            val size = onRead(sizeInBytes)
            for (i in 0 until size) audioData[offsetInBytes + i] = next++
            return size
        }
    }

    @Before
    fun setUp() {
        shadowOf(context).grantPermissions(Manifest.permission.RECORD_AUDIO)
    }

    @After
    fun tearDown() {
        ShadowAudioRecord.clearSource()
    }

    private fun feed(source: ShadowAudioRecord.AudioRecordSource) = ShadowAudioRecord.setSourceProvider { source }

    private fun TestScope.recorder() = AudioRecordVoiceRecorder(context, UnconfinedTestDispatcher(testScheduler))

    @Test
    fun record_permissionNotGranted_permissionDenied() = runTest {
        shadowOf(context).denyPermissions(Manifest.permission.RECORD_AUDIO)

        assertThat(recorder().record(300.milliseconds)).isEqualTo(Outcome.Failure(RecordingError.PERMISSION_DENIED))
    }

    @Test
    fun record_untilMaxDuration_returnsThatMuch16KhzAudio() = runTest {
        feed(CountingSource())

        val recording = (recorder().record(300.milliseconds) as Outcome.Success).data

        assertThat(recording.sampleRateHz).isEqualTo(16_000)
        assertThat(recording.pcm.size).isEqualTo(9_600)
        assertThat(recording.duration).isEqualTo(300.milliseconds)
        assertThat(recording.pcm[3_201]).isEqualTo(3_201.toByte())
    }

    @Test
    fun record_stopDuringRecording_returnsAudioSoFar() = runTest {
        val recorder = recorder()
        var reads = 0
        feed(
            CountingSource { size ->
                if (++reads == 2) recorder.stop()
                size
            }
        )

        val recording = (recorder.record(1_000.milliseconds) as Outcome.Success).data

        assertThat(recording.duration).isEqualTo(200.milliseconds)
    }

    @Test
    fun stop_beforeRecording_doesNotEndTheNextOne() = runTest {
        feed(CountingSource())
        val recorder = recorder()

        recorder.stop()
        val recording = (recorder.record(300.milliseconds) as Outcome.Success).data

        assertThat(recording.duration).isEqualTo(300.milliseconds)
    }

    @Test
    fun record_readFails_microphoneUnavailable() = runTest {
        feed(CountingSource { AudioRecord.ERROR_DEAD_OBJECT })

        assertThat(recorder().record(300.milliseconds))
            .isEqualTo(Outcome.Failure(RecordingError.MICROPHONE_UNAVAILABLE))
    }

    @Test
    fun record_cancelledWhileRecording_stopsReadingWithoutAWarning() = runTest {
        val warnings = mutableListOf<String>()
        val tree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                warnings += message
            }
        }
        Timber.plant(tree)
        var job: Job? = null
        val source = CountingSource { size ->
            job?.cancel()
            size
        }
        feed(source)
        val recorder = recorder()

        val result = async(start = CoroutineStart.LAZY) { recorder.record(1_000.milliseconds) }
        job = result
        result.start()
        runCurrent()

        Timber.uproot(tree)
        assertThat(result.isCancelled).isTrue()
        assertThat(source.reads).isEqualTo(1)
        assertThat(warnings).isEmpty()
    }

    @Test
    fun record_twice_secondRecordingIsIndependent() = runTest {
        feed(CountingSource())
        val recorder = recorder()

        recorder.record(100.milliseconds)
        val second = recorder.record(200.milliseconds)

        assertThat(second).isInstanceOf<Outcome.Success<VoiceRecording>>()
        assertThat((second as Outcome.Success).data.duration).isEqualTo(200.milliseconds)
    }

    @Test
    fun record_maxDurationOutOfRange_throws() = runTest {
        val recorder = recorder()

        listOf(Duration.ZERO, (-1).milliseconds, VoiceRecorder.MAX_DURATION + 1.milliseconds).forEach { duration ->
            assertFailure { recorder.record(duration) }.isInstanceOf<IllegalArgumentException>()
        }
    }

    @Test
    fun record_maxDurationAtLimit_records() = runTest {
        feed(CountingSource())

        val recording = (recorder().record(VoiceRecorder.MAX_DURATION) as Outcome.Success).data

        assertThat(recording.duration).isEqualTo(VoiceRecorder.MAX_DURATION)
    }
}
