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
import android.content.Intent
import android.media.AudioFormat
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import androidx.annotation.RequiresApi
import com.gyugle.gil.core.domain.Outcome
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * [OnDeviceRecognizer] over `SpeechRecognizer.createOnDeviceSpeechRecognizer`.
 *
 * Android 12 can create one but cannot say which languages are installed, so scoring starts at
 * Android 13, where `checkRecognitionSupport` can and `EXTRA_AUDIO_SOURCE` takes a recording.
 * The recording reaches the recognizer through an in-memory file, so it never touches storage.
 */
@Singleton
class AndroidOnDeviceRecognizer @Inject constructor(@ApplicationContext private val context: Context) :
    OnDeviceRecognizer {

    override suspend fun availability(language: SpeechLanguage): RecognizerAvailability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return RecognizerAvailability.UNAVAILABLE
        // SpeechRecognizer must be created and called on the main thread.
        return withContext(context.mainExecutor.asCoroutineDispatcher()) {
            withTimeoutOrNull(SUPPORT_TIMEOUT) { checkSupport(language) } ?: RecognizerAvailability.UNAVAILABLE
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun checkSupport(language: SpeechLanguage): RecognizerAvailability {
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) return RecognizerAvailability.UNAVAILABLE
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.languageTag)
            return suspendCancellableCoroutine { continuation ->
                val callback = object : RecognitionSupportCallback {
                    override fun onSupportResult(support: RecognitionSupport) {
                        val availability = recognizerAvailability(
                            installed = support.installedOnDeviceLanguages,
                            pending = support.pendingOnDeviceLanguages,
                            supported = support.supportedOnDeviceLanguages,
                            language = language
                        )
                        if (continuation.isActive) continuation.resume(availability)
                    }

                    override fun onError(error: Int) {
                        Timber.w("Recognition support check failed with code %d", error)
                        if (continuation.isActive) continuation.resume(RecognizerAvailability.UNAVAILABLE)
                    }
                }
                recognizer.checkRecognitionSupport(intent, context.mainExecutor, callback)
            }
        } finally {
            recognizer.destroy()
        }
    }

    override suspend fun recognize(
        recording: VoiceRecording,
        language: SpeechLanguage
    ): Outcome<String, RecognitionError> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return Outcome.Failure(RecognitionError.UNAVAILABLE)
        val audio = try {
            withContext(Dispatchers.IO) { inMemoryFileOf(recording.pcm) }
        } catch (e: ErrnoException) {
            Timber.w("Could not hand a recording to the recognizer: %s", e.errno)
            return Outcome.Failure(RecognitionError.FAILED)
        } catch (e: IOException) {
            Timber.w("Could not hand a recording to the recognizer: %s", e::class.simpleName)
            return Outcome.Failure(RecognitionError.FAILED)
        }
        return try {
            withContext(context.mainExecutor.asCoroutineDispatcher()) {
                withTimeoutOrNull(RECOGNITION_TIMEOUT) { listen(audio, recording.sampleRateHz, language) }
                    ?: Outcome.Failure(RecognitionError.FAILED)
            }
        } finally {
            audio.close()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun listen(
        audio: ParcelFileDescriptor,
        sampleRateHz: Int,
        language: SpeechLanguage
    ): Outcome<String, RecognitionError> {
        if (!SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            return Outcome.Failure(RecognitionError.UNAVAILABLE)
        }
        val recognizer = SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.languageTag)
                .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, audio)
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1)
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
                .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, sampleRateHz)
            return suspendCancellableCoroutine { continuation ->
                recognizer.setRecognitionListener(object : RecognitionListener {
                    var partial: List<String>? = null

                    override fun onResults(results: Bundle) {
                        val heard = heardText(results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION), partial)
                        if (continuation.isActive) continuation.resume(heard)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { partial = it }
                    }

                    override fun onError(error: Int) {
                        if (continuation.isActive) continuation.resume(Outcome.Failure(recognitionErrorOf(error)))
                    }

                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                })
                recognizer.startListening(intent)
            }
        } finally {
            recognizer.destroy()
        }
    }

    private companion object {
        val SUPPORT_TIMEOUT = 5.seconds

        /** A recording is at most a few seconds; recognizing it takes about as long again. */
        val RECOGNITION_TIMEOUT = 20.seconds
    }
}

/** A read-only stream of [pcm] from the start, backed by memory instead of a file on storage. */
@RequiresApi(Build.VERSION_CODES.R)
private fun inMemoryFileOf(pcm: ByteArray): ParcelFileDescriptor {
    val fd = Os.memfd_create("recording", 0)
    try {
        var written = 0
        while (written < pcm.size) written += Os.write(fd, pcm, written, pcm.size - written)
        Os.lseek(fd, 0, OsConstants.SEEK_SET)
        return ParcelFileDescriptor.dup(fd)
    } finally {
        Os.close(fd)
    }
}

/**
 * The best guess of a recognizer's [results], or [RecognitionError.NO_SPEECH] when it holds no words.
 *
 * Given a recording instead of the microphone, the on-device recognizer on Pixel phones sends its
 * transcript only as the last [partial] result and finishes with empty results, so that counts too.
 */
internal fun heardText(results: List<String>?, partial: List<String>? = null): Outcome<String, RecognitionError> {
    fun List<String>?.best() = this?.firstOrNull()?.trim()?.takeIf { it.isNotEmpty() }
    val heard = results.best() ?: partial.best() ?: return Outcome.Failure(RecognitionError.NO_SPEECH)
    return Outcome.Success(heard)
}

/** A `SpeechRecognizer.ERROR_*` code as a [RecognitionError]. */
internal fun recognitionErrorOf(code: Int): RecognitionError = when (code) {
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> RecognitionError.NO_SPEECH

    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
        RecognitionError.UNAVAILABLE

    else -> {
        Timber.w("Recognition failed with code %d", code)
        RecognitionError.FAILED
    }
}

/** Language lists from `RecognitionSupport`, which holds BCP 47 tags such as `en-US`. */
internal fun recognizerAvailability(
    installed: List<String>,
    pending: List<String>,
    supported: List<String>,
    language: SpeechLanguage
): RecognizerAvailability {
    val target = Locale.forLanguageTag(language.languageTag)
    fun List<String>.hasTarget() = any { Locale.forLanguageTag(it.replace('_', '-')) == target }
    return when {
        installed.hasTarget() -> RecognizerAvailability.READY
        pending.hasTarget() || supported.hasTarget() -> RecognizerAvailability.NOT_DOWNLOADED
        else -> RecognizerAvailability.UNAVAILABLE
    }
}
