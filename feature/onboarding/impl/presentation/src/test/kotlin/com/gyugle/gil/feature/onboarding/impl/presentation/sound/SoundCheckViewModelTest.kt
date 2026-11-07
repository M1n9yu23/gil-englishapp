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
package com.gyugle.gil.feature.onboarding.impl.presentation.sound

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechError
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.onboarding.impl.domain.repository.FakeOnboardingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SoundCheckViewModelTest {
    private val speaker = FakeSpeaker()
    private val onboarding = FakeOnboardingRepository()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    /** Collected for the whole test, as the screen would, so [SoundCheckViewModel.state] stays current. */
    private fun TestScope.viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        SoundCheckViewModel(handle, speaker, onboarding).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        }

    private fun voices(american: VoiceState) =
        SpeakerStatus.Connected(mapOf(SpeechLanguage.AMERICAN_ENGLISH to american))

    private fun SoundCheckViewModel.phase(): SoundCheckPhase = state.value.phase

    @Test
    fun init_voiceReady_playsHelloOnceInAmericanEnglish() = runTest {
        val viewModel = viewModel()

        assertThat(viewModel.phase()).isEqualTo(SoundCheckPhase.Ready)
        assertThat(speaker.spoken)
            .containsExactly(FakeSpeaker.Utterance("Hello", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL))
    }

    @Test
    fun init_engineStillConnecting_waitsAndPlaysWhenReady() = runTest {
        speaker.status.value = SpeakerStatus.Connecting
        val viewModel = viewModel()
        assertThat(viewModel.phase()).isEqualTo(SoundCheckPhase.Checking)
        assertThat(speaker.spoken).isEmpty()

        speaker.status.value = voices(VoiceState.INSTALLED)

        assertThat(viewModel.phase()).isEqualTo(SoundCheckPhase.Ready)
        assertThat(speaker.spoken).hasSize(1)
    }

    @Test
    fun init_noAmericanVoice_offersTheInstallAndPlaysNothing() = runTest {
        speaker.status.value = voices(VoiceState.NOT_INSTALLED)

        assertThat(viewModel().phase()).isEqualTo(SoundCheckPhase.NoVoice)
        assertThat(speaker.spoken).isEmpty()
    }

    @Test
    fun init_noEngine_saysLessonsUseText() = runTest {
        speaker.status.value = SpeakerStatus.NoEngine

        assertThat(viewModel().phase()).isEqualTo(SoundCheckPhase.NoEngine)
    }

    @Test
    fun voiceInstalledWhileAway_refreshOnResumeShowsTheWord() = runTest {
        speaker.status.value = voices(VoiceState.NOT_INSTALLED)
        val viewModel = viewModel()

        viewModel.onAction(SoundCheckAction.OnResumed)
        speaker.status.value = voices(VoiceState.INSTALLED)

        assertThat(speaker.refreshCount).isEqualTo(1)
        assertThat(viewModel.phase()).isEqualTo(SoundCheckPhase.Ready)
    }

    @Test
    fun notHeard_thenPlayAgain_backToTheQuestion() = runTest {
        val viewModel = viewModel()

        viewModel.onAction(SoundCheckAction.OnNotHeardClick)
        assertThat(viewModel.phase()).isEqualTo(SoundCheckPhase.NotHeard)

        viewModel.onAction(SoundCheckAction.OnPlayClick)
        assertThat(viewModel.phase()).isEqualTo(SoundCheckPhase.Ready)
        assertThat(speaker.spoken).hasSize(2)
    }

    @Test
    fun speakFails_readsAsNotHeard() = runTest {
        speaker.error = SpeechError.FAILED

        assertThat(viewModel().phase()).isEqualTo(SoundCheckPhase.NotHeard)
    }

    @Test
    fun heard_voiceReady_endsTheFirstRun() = runTest {
        viewModel().onAction(SoundCheckAction.OnHeardClick)

        assertThat(onboarding.completed.value).isTrue()
    }

    @Test
    fun continueWithoutSound_noEngine_endsTheFirstRunToo() = runTest {
        speaker.status.value = SpeakerStatus.NoEngine

        viewModel().onAction(SoundCheckAction.OnContinueWithoutSoundClick)

        assertThat(onboarding.completed.value).isTrue()
    }

    @Test
    fun finish_writeFails_showsTheErrorAndCanTryAgain() = runTest {
        onboarding.error = DataError.Local.UNKNOWN
        val viewModel = viewModel()

        viewModel.onAction(SoundCheckAction.OnHeardClick)
        assertThat(viewModel.state.value.error).isNotNull()
        assertThat(viewModel.state.value.isFinishing).isFalse()
        onboarding.error = null
        viewModel.onAction(SoundCheckAction.OnHeardClick)

        assertThat(onboarding.completeCount).isEqualTo(2)
        assertThat(onboarding.completed.value).isTrue()
    }

    @Test
    fun installUnavailable_explainsWhereToLook() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(SoundCheckAction.OnInstallClick)
            assertThat(awaitItem()).isEqualTo(SoundCheckEvent.OpenVoiceInstall)
        }
        viewModel.onAction(SoundCheckAction.OnInstallUnavailable)
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun processDeath_afterPlaying_doesNotPlayAgainAndKeepsTheAnswer() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).onAction(SoundCheckAction.OnNotHeardClick)

        val restored = viewModel(handle)

        assertThat(speaker.spoken).hasSize(1)
        assertThat(restored.phase()).isEqualTo(SoundCheckPhase.NotHeard)
    }
}
