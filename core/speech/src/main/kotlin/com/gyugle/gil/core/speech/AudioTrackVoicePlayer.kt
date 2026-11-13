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

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * [VoicePlayer] over a static `AudioTrack`: a recording is at most a minute of 16-bit mono, small
 * enough to hand over whole.
 */
@Singleton
class AudioTrackVoicePlayer internal constructor(private val io: CoroutineDispatcher) : VoicePlayer {

    @Inject
    constructor() : this(Dispatchers.IO)

    private val lock = Mutex()

    override suspend fun play(recording: VoiceRecording): Boolean {
        if (recording.pcm.isEmpty()) return true
        return lock.withLock { withContext(io) { playLocked(recording) } }
    }

    private suspend fun playLocked(recording: VoiceRecording): Boolean {
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(recording.sampleRateHz)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(recording.pcm.size)
                .build()
        } catch (e: UnsupportedOperationException) {
            Timber.w("Cannot play a recording: %s", e::class.simpleName)
            return false
        } catch (e: IllegalArgumentException) {
            Timber.w("Cannot play a recording: %s", e::class.simpleName)
            return false
        }
        try {
            if (track.write(recording.pcm, 0, recording.pcm.size) < 0) return false
            track.play()
            val frames = recording.pcm.size / VoiceRecording.BYTES_PER_SAMPLE
            // The head position stops at the last frame, so polling sees the end without a listener
            // thread. Cancellation lands within one poll. A device whose head never quite reaches
            // the end, or whose output stalls, is let go a little after the recording's length.
            val ended = withTimeoutOrNull(recording.duration + END_MARGIN) {
                while (track.playbackHeadPosition < frames && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    delay(POLL_MILLIS)
                }
            }
            if (ended == null) Timber.w("A recording did not report its end")
            return true
        } catch (e: IllegalStateException) {
            // CancellationException is an IllegalStateException: stopping playback is not a failure.
            currentCoroutineContext().ensureActive()
            Timber.w("Cannot play a recording: %s", e::class.simpleName)
            return false
        } finally {
            track.release()
        }
    }

    private companion object {
        const val POLL_MILLIS = 50L
        val END_MARGIN = 500.milliseconds
    }
}
