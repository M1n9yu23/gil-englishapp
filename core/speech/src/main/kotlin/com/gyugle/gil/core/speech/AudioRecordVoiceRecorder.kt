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
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.gyugle.gil.core.domain.Outcome
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * [VoiceRecorder] over `AudioRecord` at 16 kHz, the rate speech recognizers expect.
 *
 * The source is `VOICE_RECOGNITION`, which skips the noise suppression and gain control `MIC`
 * may apply, so the learner hears their own voice as it was.
 */
@Singleton
class AudioRecordVoiceRecorder internal constructor(private val context: Context, private val io: CoroutineDispatcher) :
    VoiceRecorder {

    @Inject
    constructor(@ApplicationContext context: Context) : this(context, Dispatchers.IO)

    private val lock = Mutex()

    /** The stop flag of the recording in progress, so a late [stop] cannot end the next one. */
    private val activeStop = AtomicReference<AtomicBoolean?>(null)

    override suspend fun record(maxDuration: Duration): Outcome<VoiceRecording, RecordingError> {
        require(maxDuration.isPositive() && maxDuration <= VoiceRecorder.MAX_DURATION) {
            "maxDuration must be in (0, ${VoiceRecorder.MAX_DURATION}], was $maxDuration"
        }
        return lock.withLock { withContext(io) { recordLocked(maxDuration) } }
    }

    override fun stop() {
        activeStop.get()?.set(true)
    }

    // The permission is checked first; AudioRecord still throws SecurityException if it is revoked
    // in between, which is caught.
    @SuppressLint("MissingPermission")
    private suspend fun recordLocked(maxDuration: Duration): Outcome<VoiceRecording, RecordingError> {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
        if (granted != PackageManager.PERMISSION_GRANTED) return Outcome.Failure(RecordingError.PERMISSION_DENIED)

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE_HZ, CHANNEL, ENCODING)
        if (minBuffer <= 0) return unavailable("no buffer size for 16 kHz mono")
        val stopRequested = AtomicBoolean(false)
        activeStop.set(stopRequested)
        val audioRecord = try {
            AudioRecord(SOURCE, SAMPLE_RATE_HZ, CHANNEL, ENCODING, maxOf(minBuffer, CHUNK_BYTES * 2))
        } catch (_: SecurityException) {
            activeStop.compareAndSet(stopRequested, null)
            return Outcome.Failure(RecordingError.PERMISSION_DENIED)
        } catch (_: IllegalArgumentException) {
            activeStop.compareAndSet(stopRequested, null)
            return unavailable("format rejected")
        }
        try {
            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) return unavailable("not initialized")
            audioRecord.startRecording()
            if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) return unavailable("did not start")

            val maxSamples = maxDuration.inWholeMilliseconds * SAMPLE_RATE_HZ / 1_000
            val maxBytes = (maxSamples * VoiceRecording.BYTES_PER_SAMPLE).toInt()
            val pcm = ByteArray(maxBytes)
            var size = 0
            while (size < maxBytes && !stopRequested.get()) {
                currentCoroutineContext().ensureActive()
                // Blocks for at most one chunk, which bounds how late stop and cancellation land.
                val read = audioRecord.read(pcm, size, minOf(CHUNK_BYTES, maxBytes - size))
                if (read < 0) return unavailable("read failed with $read")
                size += read
            }
            return Outcome.Success(VoiceRecording(if (size == maxBytes) pcm else pcm.copyOf(size), SAMPLE_RATE_HZ))
        } catch (_: IllegalStateException) {
            return unavailable("start failed")
        } finally {
            activeStop.compareAndSet(stopRequested, null)
            audioRecord.release()
        }
    }

    private fun unavailable(reason: String): Outcome.Failure<RecordingError> {
        Timber.w("Microphone unavailable: %s", reason)
        return Outcome.Failure(RecordingError.MICROPHONE_UNAVAILABLE)
    }

    private companion object {
        const val SOURCE = MediaRecorder.AudioSource.VOICE_RECOGNITION
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** 100 ms of audio. */
        const val CHUNK_BYTES = SAMPLE_RATE_HZ / 10 * VoiceRecording.BYTES_PER_SAMPLE
    }
}
