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
import android.os.Build
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * [OnDeviceRecognizer] over `SpeechRecognizer.createOnDeviceSpeechRecognizer`.
 *
 * Android 12 can create one but cannot say which languages are installed, so scoring starts at
 * Android 13, where `checkRecognitionSupport` can.
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

    private companion object {
        val SUPPORT_TIMEOUT = 5.seconds
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
