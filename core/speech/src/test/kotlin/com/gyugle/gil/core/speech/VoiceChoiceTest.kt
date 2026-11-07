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

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.util.Locale
import org.junit.jupiter.api.Test

class VoiceChoiceTest {

    private fun voice(
        name: String,
        locale: Locale = Locale.US,
        quality: Int = 300,
        latency: Int = 300,
        needsNetwork: Boolean = false,
        installed: Boolean = true
    ) = VoiceCandidate(name, locale, quality, latency, needsNetwork, installed)

    @Test
    fun chooseVoice_noVoices_unsupported() {
        assertThat(chooseVoice(emptyList(), SpeechLanguage.AMERICAN_ENGLISH))
            .isEqualTo(VoiceChoice(VoiceState.UNSUPPORTED, null))
    }

    @Test
    fun chooseVoice_onlyOtherLanguages_unsupported() {
        val voices = listOf(voice("ko", Locale.KOREA), voice("gb", Locale.UK))

        assertThat(chooseVoice(voices, SpeechLanguage.AMERICAN_ENGLISH))
            .isEqualTo(VoiceChoice(VoiceState.UNSUPPORTED, null))
    }

    @Test
    fun chooseVoice_onlyNetworkVoice_notInstalled() {
        val voices = listOf(voice("en-us-network", needsNetwork = true))

        assertThat(chooseVoice(voices, SpeechLanguage.AMERICAN_ENGLISH))
            .isEqualTo(VoiceChoice(VoiceState.NOT_INSTALLED, null))
    }

    @Test
    fun chooseVoice_offlineVoiceNotDownloaded_notInstalled() {
        val voices = listOf(voice("en-us-local", installed = false))

        assertThat(chooseVoice(voices, SpeechLanguage.AMERICAN_ENGLISH))
            .isEqualTo(VoiceChoice(VoiceState.NOT_INSTALLED, null))
    }

    @Test
    fun chooseVoice_networkVoiceBetterThanOffline_picksOffline() {
        val voices = listOf(
            voice("en-us-network", quality = 500, needsNetwork = true),
            voice("en-us-local", quality = 300)
        )

        assertThat(chooseVoice(voices, SpeechLanguage.AMERICAN_ENGLISH))
            .isEqualTo(VoiceChoice(VoiceState.INSTALLED, "en-us-local"))
    }

    @Test
    fun chooseVoice_severalOffline_highestQualityThenLowestLatencyThenName() {
        val voices = listOf(
            voice("c", quality = 400, latency = 200),
            voice("b", quality = 400, latency = 300),
            voice("a", quality = 300, latency = 100),
            voice("d", quality = 400, latency = 200)
        )

        assertThat(chooseVoice(voices, SpeechLanguage.AMERICAN_ENGLISH).voiceName).isEqualTo("c")
    }

    // Some engines build their locales this way; Locale.Builder rejects a three-letter region.
    @Suppress("DEPRECATION")
    @Test
    fun chooseVoice_threeLetterLocale_matchesTwoLetterLanguage() {
        val voices = listOf(voice("eng-usa", Locale("eng", "USA")))

        assertThat(chooseVoice(voices, SpeechLanguage.AMERICAN_ENGLISH))
            .isEqualTo(VoiceChoice(VoiceState.INSTALLED, "eng-usa"))
    }

    @Test
    fun chooseVoice_britishRequested_ignoresAmericanVoice() {
        val voices = listOf(voice("us", Locale.US), voice("gb", Locale.UK))

        assertThat(chooseVoice(voices, SpeechLanguage.BRITISH_ENGLISH).voiceName).isEqualTo("gb")
    }

    @Test
    fun chooseVoice_korean_matchesKoreaLocale() {
        val voices = listOf(voice("ko", Locale.KOREA))

        assertThat(chooseVoice(voices, SpeechLanguage.KOREAN).voiceName).isEqualTo("ko")
    }

    @Test
    fun chooseVoice_languageWithoutCountry_doesNotMatch() {
        val voices = listOf(voice("en", Locale.ENGLISH))

        assertThat(chooseVoice(voices, SpeechLanguage.AMERICAN_ENGLISH).state).isEqualTo(VoiceState.UNSUPPORTED)
    }
}
