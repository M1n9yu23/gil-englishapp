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
package com.gyugle.gil.feature.settings.impl.presentation.sound

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.impl.domain.repository.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SoundViewModelTest {
    private val speaker = FakeSpeaker()
    private val settings = FakeSettingsRepository()

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SoundViewModel(speaker, settings)

    @Test
    fun pick_savesAccentAndSpeed() {
        val viewModel = viewModel()

        viewModel.onAction(SoundAction.OnAccentPick(Accent.BRITISH))
        viewModel.onAction(SoundAction.OnSpeedPick(SpeechSpeed.SLOW))

        assertThat(viewModel.state.value.accent).isEqualTo(Accent.BRITISH)
        assertThat(viewModel.state.value.speed).isEqualTo(SpeechSpeed.SLOW)
    }

    @Test
    fun play_usesTheChosenAccentAndSpeed() {
        val viewModel = viewModel()
        viewModel.onAction(SoundAction.OnAccentPick(Accent.BRITISH))
        viewModel.onAction(SoundAction.OnSpeedPick(SpeechSpeed.SLOW))

        viewModel.onAction(SoundAction.OnPlayClick)

        assertThat(speaker.spoken.single()).isEqualTo(
            FakeSpeaker.Utterance("Nice to meet you.", SpeechLanguage.BRITISH_ENGLISH, SpeechRate.SLOW)
        )
    }

    @Test
    fun voices_showWhatThePhoneHas() {
        speaker.status.value = SpeakerStatus.Connected(
            mapOf(
                SpeechLanguage.AMERICAN_ENGLISH to VoiceState.INSTALLED,
                SpeechLanguage.BRITISH_ENGLISH to VoiceState.NOT_INSTALLED
            )
        )
        val viewModel = viewModel()

        assertThat(
            viewModel.state.value.voices
        ).isEqualTo(VoicesUi.Known(VoiceState.INSTALLED, VoiceState.NOT_INSTALLED))

        speaker.status.value = SpeakerStatus.NoEngine
        assertThat(viewModel.state.value.voices).isEqualTo(VoicesUi.NoEngine)
    }

    @Test
    fun resumed_readsTheVoicesAgain() {
        val viewModel = viewModel()

        viewModel.onAction(SoundAction.OnResumed)

        assertThat(speaker.refreshCount).isEqualTo(1)
    }
}
