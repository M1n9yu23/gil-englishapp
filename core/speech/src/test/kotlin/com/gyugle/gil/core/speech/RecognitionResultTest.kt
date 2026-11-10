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

import android.speech.SpeechRecognizer
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.domain.Outcome
import org.junit.jupiter.api.Test

class RecognitionResultTest {

    @Test
    fun heardText_firstResult_trimmed() {
        assertThat(heardText(listOf(" nice to meet you ", "nice to meet you two")))
            .isEqualTo(Outcome.Success("nice to meet you"))
    }

    @Test
    fun heardText_noResults_noSpeech() {
        assertThat(heardText(null)).isEqualTo(Outcome.Failure(RecognitionError.NO_SPEECH))
        assertThat(heardText(emptyList())).isEqualTo(Outcome.Failure(RecognitionError.NO_SPEECH))
    }

    @Test
    fun heardText_blankResult_noSpeech() {
        assertThat(heardText(listOf("  "))).isEqualTo(Outcome.Failure(RecognitionError.NO_SPEECH))
    }

    @Test
    fun heardText_emptyResultsAfterPartial_usesThePartial() {
        assertThat(heardText(emptyList(), partial = listOf("Nice to meet you")))
            .isEqualTo(Outcome.Success("Nice to meet you"))
        assertThat(heardText(null, partial = listOf("Nice to meet you")))
            .isEqualTo(Outcome.Success("Nice to meet you"))
    }

    @Test
    fun heardText_finalResults_winOverThePartial() {
        assertThat(
            heardText(listOf("nice to meet you"), partial = listOf("nice to"))
        ).isEqualTo(Outcome.Success("nice to meet you"))
    }

    @Test
    fun heardText_blankPartial_noSpeech() {
        assertThat(heardText(emptyList(), partial = listOf(""))).isEqualTo(Outcome.Failure(RecognitionError.NO_SPEECH))
    }

    @Test
    fun recognitionErrorOf_noMatchOrTimeout_noSpeech() {
        assertThat(recognitionErrorOf(SpeechRecognizer.ERROR_NO_MATCH)).isEqualTo(RecognitionError.NO_SPEECH)
        assertThat(recognitionErrorOf(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)).isEqualTo(RecognitionError.NO_SPEECH)
    }

    @Test
    fun recognitionErrorOf_languageMissing_unavailable() {
        assertThat(recognitionErrorOf(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED))
            .isEqualTo(RecognitionError.UNAVAILABLE)
        assertThat(recognitionErrorOf(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE))
            .isEqualTo(RecognitionError.UNAVAILABLE)
    }

    @Test
    fun recognitionErrorOf_otherCodes_failed() {
        assertThat(recognitionErrorOf(SpeechRecognizer.ERROR_CLIENT)).isEqualTo(RecognitionError.FAILED)
        assertThat(recognitionErrorOf(SpeechRecognizer.ERROR_SERVER_DISCONNECTED)).isEqualTo(RecognitionError.FAILED)
    }
}
