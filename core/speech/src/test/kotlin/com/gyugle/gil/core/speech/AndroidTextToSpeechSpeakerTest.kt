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

import android.app.Application
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.Outcome
import java.io.File
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowTextToSpeech

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class AndroidTextToSpeechSpeakerTest {

    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        ShadowTextToSpeech.addVoice(
            voice("en-us-network", Locale.US, quality = Voice.QUALITY_VERY_HIGH, network = true)
        )
        ShadowTextToSpeech.addVoice(voice("en-us-local", Locale.US))
        ShadowTextToSpeech.addVoice(voice("en-gb-network", Locale.UK, network = true))
    }

    private fun voice(name: String, locale: Locale, quality: Int = Voice.QUALITY_HIGH, network: Boolean = false) =
        Voice(name, locale, quality, Voice.LATENCY_NORMAL, network, emptySet())

    // Tests run unconfined so each call reaches the engine before the test answers for the engine.
    private fun TestScope.speaker(scope: CoroutineScope = backgroundScope) =
        AndroidTextToSpeechSpeaker(context, scope, UnconfinedTestDispatcher(testScheduler))

    private fun lastEngine(): ShadowTextToSpeech = shadowOf(ShadowTextToSpeech.getLastTextToSpeechInstance())

    private fun ShadowTextToSpeech.init(code: Int = TextToSpeech.SUCCESS) = onInitListener.onInit(code)

    private fun idleMainLooper() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun status_beforeEngineAnswers_connecting() = runTest(UnconfinedTestDispatcher()) {
        assertThat(speaker().status.value).isEqualTo(SpeakerStatus.Connecting)
    }

    @Test
    fun status_engineFailsToStart_noEngine() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init(TextToSpeech.ERROR)

        assertThat(speaker.status.value).isEqualTo(SpeakerStatus.NoEngine)
    }

    @Test
    fun status_engineStarts_reportsEachLanguage() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()

        val status = speaker.status.value as SpeakerStatus.Connected
        assertThat(status.voice(SpeechLanguage.AMERICAN_ENGLISH)).isEqualTo(VoiceState.INSTALLED)
        assertThat(status.voice(SpeechLanguage.BRITISH_ENGLISH)).isEqualTo(VoiceState.NOT_INSTALLED)
        assertThat(status.voice(SpeechLanguage.KOREAN)).isEqualTo(VoiceState.UNSUPPORTED)
    }

    @Test
    fun speak_offlineVoiceInstalled_speaksWithThatVoiceAndRate() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val engine = lastEngine()
        engine.init()

        val result = async { speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.SLOW) }
        idleMainLooper()

        assertThat(result.await()).isEqualTo(Outcome.Success(Unit))
        assertThat(engine.lastSpokenText).isEqualTo("Hello.")
        assertThat(engine.currentVoice.name).isEqualTo("en-us-local")
        assertThat(engine.queueMode).isEqualTo(TextToSpeech.QUEUE_FLUSH)
    }

    @Test
    fun speak_onlyNetworkVoice_voiceNotInstalledAndNothingSpoken() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val engine = lastEngine()
        engine.init()

        assertThat(speaker.speak("Hello.", SpeechLanguage.BRITISH_ENGLISH))
            .isEqualTo(Outcome.Failure(SpeechError.VOICE_NOT_INSTALLED))
        assertThat(engine.spokenTextList.isEmpty()).isTrue()
    }

    @Test
    fun speak_noEngine_noEngine() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init(TextToSpeech.ERROR)

        assertThat(speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH))
            .isEqualTo(Outcome.Failure(SpeechError.NO_ENGINE))
    }

    @Test
    fun speak_engineNeverAnswers_noEngineAfterTimeout() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()

        val result = async { speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH) }
        advanceTimeBy(5_001)

        assertThat(result.await()).isEqualTo(Outcome.Failure(SpeechError.NO_ENGINE))
    }

    @Test
    fun speak_engineReportsError_failed() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val engine = lastEngine()
        engine.init()

        val result = async { speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH) }
        engine.utteranceProgressListener.onError("gil-1", TextToSpeech.ERROR_SYNTHESIS)

        assertThat(result.await()).isEqualTo(Outcome.Failure(SpeechError.FAILED))
    }

    @Test
    fun speak_interruptedByLaterSpeech_success() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val engine = lastEngine()
        engine.init()

        val first = async { speaker.speak("One.", SpeechLanguage.AMERICAN_ENGLISH) }
        engine.utteranceProgressListener.onStop("gil-1", true)

        assertThat(first.await()).isEqualTo(Outcome.Success(Unit))
    }

    @Test
    fun speak_cancelled_stopsEngine() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val engine = lastEngine()
        engine.init()

        val result = async { speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH) }
        result.cancel()

        assertThat(engine.isStopped).isTrue()
    }

    @Test
    fun speak_olderCallCancelledAfterNewerStarted_doesNotStopNewer() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val engine = lastEngine()
        engine.init()

        val first = async { speaker.speak("One.", SpeechLanguage.AMERICAN_ENGLISH) }
        val second = async { speaker.speak("Two.", SpeechLanguage.AMERICAN_ENGLISH) }
        first.cancel()

        assertThat(engine.isStopped).isFalse()
        idleMainLooper()
        assertThat(second.await()).isEqualTo(Outcome.Success(Unit))
    }

    @Test
    fun refresh_afterNoEngine_connectsAgain() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val failed = lastEngine()
        failed.init(TextToSpeech.ERROR)

        speaker.refresh()
        val retried = lastEngine()
        retried.init()

        assertThat(retried).isNotSameInstanceAs(failed)
        assertThat(failed.isShutdown).isTrue()
        assertThat(speaker.status.value).isInstanceOf<SpeakerStatus.Connected>()
    }

    @Test
    fun refresh_voiceInstalledMeanwhile_reportsIt() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()
        ShadowTextToSpeech.addVoice(voice("ko-kr-local", Locale.KOREA))

        speaker.refresh()

        val status = speaker.status.value as SpeakerStatus.Connected
        assertThat(status.voice(SpeechLanguage.KOREAN)).isEqualTo(VoiceState.INSTALLED)
    }

    @Test
    fun refresh_staleEngineAnswersLate_ignored() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val failed = lastEngine()
        failed.init(TextToSpeech.ERROR)
        speaker.refresh()

        failed.init(TextToSpeech.SUCCESS)

        assertThat(speaker.status.value).isEqualTo(SpeakerStatus.Connecting)
    }

    @Test
    fun synthesizeToFile_engineFinishes_writesFileWithSecondEngine() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val playback = lastEngine()
        playback.init()
        val file = File(context.cacheDir, "listening/one.wav")

        val result =
            async { speaker.synthesizeToFile("One.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file) }
        val files = lastEngine()
        files.init()
        files.utteranceProgressListener.onDone("gil-1")

        assertThat(result.await()).isEqualTo(Outcome.Success(Unit))
        assertThat(files).isNotSameInstanceAs(playback)
        assertThat(files.lastSynthesizeToFileText).isEqualTo("One.")
        assertThat(file.exists()).isTrue()
    }

    @Test
    fun synthesizeToFile_engineReportsError_deletesPartialFile() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()
        val file = File(context.cacheDir, "listening/two.wav")

        val result =
            async { speaker.synthesizeToFile("Two.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file) }
        val files = lastEngine()
        files.init()
        files.utteranceProgressListener.onError("gil-1", TextToSpeech.ERROR_OUTPUT)

        assertThat(result.await()).isEqualTo(Outcome.Failure(SpeechError.FAILED))
        assertThat(file.exists()).isFalse()
        assertThat(File(file.parentFile, "two.wav.part").exists()).isFalse()
    }

    @Test
    fun synthesizeToFile_cancelled_deletesPartialFile() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()
        val file = File(context.cacheDir, "listening/three.wav")

        val result =
            async { speaker.synthesizeToFile("Three.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file) }
        lastEngine().init()
        result.cancel()

        assertThat(file.exists()).isFalse()
    }

    @Test
    fun status_engineNeverAnswers_noEngineAfterTimeout() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()

        advanceTimeBy(5_001)

        assertThat(speaker.status.value).isEqualTo(SpeakerStatus.NoEngine)
    }

    @Test
    fun refresh_afterConnectTimeout_connectsAgain() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val stuck = lastEngine()
        advanceTimeBy(5_001)

        speaker.refresh()
        lastEngine().init()

        assertThat(lastEngine()).isNotSameInstanceAs(stuck)
        assertThat(speaker.status.value).isInstanceOf<SpeakerStatus.Connected>()
    }

    @Test
    fun speak_engineNeverFinishes_failsAndReconnects() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val engine = lastEngine()
        engine.init()

        val result = async { speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH) }
        advanceTimeBy(TextToSpeechConnection.finishTimeout("Hello.").inWholeMilliseconds + 1)

        assertThat(result.await()).isEqualTo(Outcome.Failure(SpeechError.FAILED))
        assertThat(engine.isShutdown).isTrue()
        assertThat(speaker.status.value).isEqualTo(SpeakerStatus.Connecting)
    }

    @Test
    fun speak_newerSpeechWithoutStopCallback_olderReturnsSuccess() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()

        val first = async { speaker.speak("One.", SpeechLanguage.AMERICAN_ENGLISH) }
        async { speaker.speak("Two.", SpeechLanguage.AMERICAN_ENGLISH) }

        assertThat(first.isCompleted).isTrue()
        assertThat(first.await()).isEqualTo(Outcome.Success(Unit))
    }

    @Test
    fun synthesizeToFile_interrupted_failsAndKeepsNoFile() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()
        val file = File(context.cacheDir, "listening/four.wav")

        val result =
            async { speaker.synthesizeToFile("Four.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file) }
        val files = lastEngine()
        files.init()
        files.utteranceProgressListener.onStop("gil-1", true)

        assertThat(result.await()).isEqualTo(Outcome.Failure(SpeechError.FAILED))
        assertThat(file.exists()).isFalse()
    }

    @Test
    fun synthesizeToFile_fails_keepsEarlierFile() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()
        val file = File(context.cacheDir, "listening/five.wav").apply {
            parentFile?.mkdirs()
            writeText("earlier")
        }

        val result =
            async { speaker.synthesizeToFile("Five.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file) }
        lastEngine().init(TextToSpeech.ERROR)

        assertThat(result.await()).isEqualTo(Outcome.Failure(SpeechError.NO_ENGINE))
        assertThat(file.readText()).isEqualTo("earlier")
    }

    @Test
    fun synthesizeToFile_succeeds_replacesEarlierFile() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()
        val file = File(context.cacheDir, "listening/six.wav").apply {
            parentFile?.mkdirs()
            writeText("earlier")
        }

        val result =
            async { speaker.synthesizeToFile("Six.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, file) }
        val files = lastEngine()
        files.init()
        files.utteranceProgressListener.onDone("gil-1")

        assertThat(result.await()).isEqualTo(Outcome.Success(Unit))
        assertThat(file.readText().trim()).isEqualTo("Six.")
        assertThat(File(file.parentFile, "six.wav.part").exists()).isFalse()
    }

    @Test
    fun synthesizeToFile_twoCalls_secondStartsAfterFirstEnds() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        lastEngine().init()
        val dir = File(context.cacheDir, "listening")

        val first =
            async {
                speaker.synthesizeToFile("One.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, File(dir, "1.wav"))
            }
        val files = lastEngine()
        files.init()
        val second =
            async {
                speaker.synthesizeToFile("Two.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL, File(dir, "2.wav"))
            }

        assertThat(files.lastSynthesizeToFileText).isEqualTo("One.")
        files.utteranceProgressListener.onDone("gil-1")
        assertThat(files.lastSynthesizeToFileText).isEqualTo("Two.")
        files.utteranceProgressListener.onDone("gil-2")

        assertThat(first.await()).isEqualTo(Outcome.Success(Unit))
        assertThat(second.await()).isEqualTo(Outcome.Success(Unit))
    }

    @Test
    fun speak_whileFileIsWritten_usesTheOtherEngine() = runTest(UnconfinedTestDispatcher()) {
        val speaker = speaker()
        val playback = lastEngine()
        playback.init()

        val file = async {
            speaker.synthesizeToFile(
                "One.",
                SpeechLanguage.AMERICAN_ENGLISH,
                SpeechRate.NORMAL,
                File(context.cacheDir, "7.wav")
            )
        }
        val files = lastEngine()
        files.init()
        val spoken = async { speaker.speak("Hello.", SpeechLanguage.AMERICAN_ENGLISH) }
        idleMainLooper()
        files.utteranceProgressListener.onDone("gil-1")

        assertThat(spoken.await()).isEqualTo(Outcome.Success(Unit))
        assertThat(file.await()).isEqualTo(Outcome.Success(Unit))
        assertThat(playback.lastSpokenText).isEqualTo("Hello.")
        assertThat(files.spokenTextList.isEmpty()).isTrue()
    }
}
