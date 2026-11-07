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

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * One `TextToSpeech` instance: its connection, its offline voices and its utterance callbacks.
 *
 * The engine runs in another process and can die at any time (an update, low memory), after which
 * no callback ever arrives. Every wait here is therefore bounded: connecting by [CONNECT_TIMEOUT],
 * an utterance by a limit that grows with its length.
 *
 * Callbacks arrive on the main thread and on binder threads, so the connection state is guarded
 * by [lock]. Voice and rate are engine-wide settings, so they are set together with the start of
 * each utterance under [startLock].
 */
internal class TextToSpeechConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher
) {
    enum class Kind(val whenStopped: EmptyOutcome<SpeechError>) {
        /** Replaces whatever is playing. An interrupted utterance counts as heard. */
        SPEECH(Outcome.Success(Unit)),

        /** Waits for the previous one. An interrupted file is incomplete. */
        FILE(Outcome.Failure(SpeechError.FAILED))
    }

    private val mutableStatus = MutableStateFlow<SpeakerStatus>(SpeakerStatus.Connecting)
    val status: StateFlow<SpeakerStatus> = mutableStatus.asStateFlow()

    private val lock = Any()

    /** Bumped on every connect, so callbacks from a replaced engine are ignored. Guarded by [lock]. */
    private var generation = 0
    private var engine: TextToSpeech? = null
    private var voices: Map<SpeechLanguage, Voice> = emptyMap()

    @Volatile
    private var currentUtteranceId: String? = null

    private val nextUtteranceId = AtomicLong()
    private val pending = ConcurrentHashMap<String, CompletableDeferred<UtteranceEnd>>()
    private val startLock = Mutex()

    fun connect() {
        val connection = synchronized(lock) {
            generation++
            engine?.shutdown()
            voices = emptyMap()
            mutableStatus.value = SpeakerStatus.Connecting
            // The old engine will never answer these.
            pending.values.forEach { it.complete(UtteranceEnd.ERROR) }
            val connection = generation
            // The platform reports a missing engine from inside the constructor; the lock is
            // reentrant, so that onInit runs here. Success always arrives later.
            engine = TextToSpeech(context) { code -> onInit(connection, code) }.apply {
                setOnUtteranceProgressListener(ProgressListener())
            }
            connection
        }
        scope.launch {
            delay(CONNECT_TIMEOUT)
            synchronized(lock) {
                if (connection == generation && mutableStatus.value == SpeakerStatus.Connecting) {
                    Timber.w("Speech engine did not connect")
                    mutableStatus.value = SpeakerStatus.NoEngine
                }
            }
        }
    }

    fun refresh() {
        synchronized(lock) {
            when (mutableStatus.value) {
                SpeakerStatus.Connecting -> Unit

                SpeakerStatus.NoEngine -> connect()

                is SpeakerStatus.Connected -> {
                    val connection = generation
                    scope.launch(io) { readVoices(connection) }
                }
            }
        }
    }

    /**
     * Starts one utterance of [text] with [start] and suspends until the engine reports its end.
     * [start] returns the `TextToSpeech.SUCCESS` or `ERROR` code of the platform call.
     */
    suspend fun utter(
        text: String,
        language: SpeechLanguage,
        rate: SpeechRate,
        kind: Kind,
        start: (TextToSpeech, utteranceId: String) -> Int
    ): EmptyOutcome<SpeechError> {
        val settled = status.first { it != SpeakerStatus.Connecting }
        if (settled !is SpeakerStatus.Connected) return Outcome.Failure(SpeechError.NO_ENGINE)
        val (connection, tts, voice) = synchronized(lock) { Triple(generation, engine, voices[language]) }
        if (tts == null) return Outcome.Failure(SpeechError.NO_ENGINE)
        if (voice == null) return Outcome.Failure(SpeechError.VOICE_NOT_INSTALLED)

        val id = "gil-${nextUtteranceId.incrementAndGet()}"
        val end = CompletableDeferred<UtteranceEnd>()
        pending[id] = end
        try {
            val started = startLock.withLock {
                withContext(io) {
                    currentUtteranceId = id
                    tts.setVoice(voice) == TextToSpeech.SUCCESS &&
                        tts.setSpeechRate(rate.multiplier) == TextToSpeech.SUCCESS &&
                        start(tts, id) == TextToSpeech.SUCCESS
                }
            }
            if (!started) {
                Timber.w("Speech engine refused an utterance")
                return Outcome.Failure(SpeechError.FAILED)
            }
            if (kind == Kind.SPEECH) endOlderThan(id)

            return when (withTimeoutOrNull(finishTimeout(text)) { end.await() }) {
                UtteranceEnd.DONE -> Outcome.Success(Unit)

                UtteranceEnd.STOPPED -> kind.whenStopped

                UtteranceEnd.ERROR -> Outcome.Failure(SpeechError.FAILED)

                null -> {
                    Timber.w("Speech engine did not finish an utterance; reconnecting")
                    stopIfCurrent(tts, id)
                    synchronized(lock) { if (connection == generation) connect() }
                    Outcome.Failure(SpeechError.FAILED)
                }
            }
        } catch (e: CancellationException) {
            withContext(NonCancellable) { stopIfCurrent(tts, id) }
            throw e
        } finally {
            pending.remove(id)
        }
    }

    /** QUEUE_FLUSH has dropped every older utterance, but not every engine reports that with onStop. */
    private fun endOlderThan(id: String) {
        pending.forEach { (other, older) -> if (other != id) older.complete(UtteranceEnd.STOPPED) }
    }

    /** Stops [id] unless a newer utterance has started, which stopping would cut off. */
    private suspend fun stopIfCurrent(tts: TextToSpeech, id: String) {
        startLock.withLock { if (currentUtteranceId == id) tts.stop() }
    }

    private fun onInit(connection: Int, code: Int) {
        synchronized(lock) {
            if (connection != generation) return
            if (code != TextToSpeech.SUCCESS) {
                Timber.w("Speech engine failed to start with code %d", code)
                mutableStatus.value = SpeakerStatus.NoEngine
                return
            }
        }
        scope.launch(io) { readVoices(connection) }
    }

    private fun readVoices(connection: Int) {
        val tts = synchronized(lock) { engine.takeIf { connection == generation } } ?: return
        // A binder call, so outside the lock. Null when the engine has died.
        val available = tts.voices
        synchronized(lock) {
            if (connection != generation) return
            if (available == null) {
                Timber.w("Speech engine returned no voice list")
                voices = emptyMap()
                mutableStatus.value = SpeakerStatus.NoEngine
                return
            }
            val candidates = available.map { it.toCandidate() }
            val choices = SpeechLanguage.entries.associateWith { chooseVoice(candidates, it) }
            voices = choices.mapNotNull { (language, choice) ->
                available.firstOrNull { it.name == choice.voiceName }?.let { language to it }
            }.toMap()
            mutableStatus.value = SpeakerStatus.Connected(choices.mapValues { it.value.state })
        }
    }

    private enum class UtteranceEnd { DONE, STOPPED, ERROR }

    private inner class ProgressListener : UtteranceProgressListener() {
        override fun onStart(utteranceId: String) = Unit

        override fun onDone(utteranceId: String) {
            pending[utteranceId]?.complete(UtteranceEnd.DONE)
        }

        override fun onStop(utteranceId: String, interrupted: Boolean) {
            pending[utteranceId]?.complete(UtteranceEnd.STOPPED)
        }

        @Deprecated("Replaced by onError(String, Int) in API 21; still called by older engines.")
        override fun onError(utteranceId: String) {
            pending[utteranceId]?.complete(UtteranceEnd.ERROR)
        }

        override fun onError(utteranceId: String, errorCode: Int) {
            Timber.w("Speech engine error %d", errorCode)
            pending[utteranceId]?.complete(UtteranceEnd.ERROR)
        }
    }

    internal companion object {
        /** Binding normally takes well under a second; a stuck engine must not hang a lesson. */
        val CONNECT_TIMEOUT = 5.seconds

        /**
         * Far longer than speaking [text] slowly takes, so only an engine that stopped answering
         * reaches it.
         */
        fun finishTimeout(text: String): Duration = 10.seconds + 150.milliseconds * text.length
    }
}

private fun Voice.toCandidate() = VoiceCandidate(
    name = name,
    locale = locale,
    quality = quality,
    latency = latency,
    needsNetwork = isNetworkConnectionRequired,
    installed = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features.orEmpty()
)
