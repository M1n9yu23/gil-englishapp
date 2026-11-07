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
import org.junit.jupiter.api.Test

class RecognizerAvailabilityTest {

    private fun availability(
        installed: List<String> = emptyList(),
        pending: List<String> = emptyList(),
        supported: List<String> = emptyList(),
        language: SpeechLanguage = SpeechLanguage.AMERICAN_ENGLISH
    ) = recognizerAvailability(installed, pending, supported, language)

    @Test
    fun recognizerAvailability_installed_ready() {
        assertThat(availability(installed = listOf("ko-KR", "en-US"), supported = listOf("en-US")))
            .isEqualTo(RecognizerAvailability.READY)
    }

    @Test
    fun recognizerAvailability_onlySupported_notDownloaded() {
        assertThat(availability(supported = listOf("en-US"))).isEqualTo(RecognizerAvailability.NOT_DOWNLOADED)
    }

    @Test
    fun recognizerAvailability_downloading_notDownloaded() {
        assertThat(availability(pending = listOf("en-US"))).isEqualTo(RecognizerAvailability.NOT_DOWNLOADED)
    }

    @Test
    fun recognizerAvailability_otherLanguagesOnly_unavailable() {
        assertThat(availability(installed = listOf("en-GB"), supported = listOf("ko-KR")))
            .isEqualTo(RecognizerAvailability.UNAVAILABLE)
    }

    @Test
    fun recognizerAvailability_emptyLists_unavailable() {
        assertThat(availability()).isEqualTo(RecognizerAvailability.UNAVAILABLE)
    }

    @Test
    fun recognizerAvailability_underscoreOrLowercaseTag_ready() {
        assertThat(availability(installed = listOf("en_US"))).isEqualTo(RecognizerAvailability.READY)
        assertThat(availability(installed = listOf("en-us"))).isEqualTo(RecognizerAvailability.READY)
    }
}
