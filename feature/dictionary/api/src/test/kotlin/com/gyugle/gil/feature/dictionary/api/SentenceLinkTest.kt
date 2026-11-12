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
package com.gyugle.gil.feature.dictionary.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class SentenceLinkTest {
    @Test
    fun parse_aLinkItMade_readsItBack() {
        val link = SentenceLink(sentenceId = 42, entryId = "pattern.how-are-you")

        assertThat(SentenceLink.parse(link.uri)).isEqualTo(link)
    }

    @Test
    fun uri_staysFixed_soPinnedWidgetsStillOpen() {
        assertThat(SentenceLink(3, "word.run").uri).isEqualTo("gil://sentence/3/word.run")
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "gil://sentence/3",
            "gil://sentence/3/word.run/extra",
            "gil://sentence/0/word.run",
            "gil://sentence/-3/word.run",
            "gil://sentence/03/word.run",
            "gil://sentence/٣/word.run",
            "gil://sentence/99999999999999999999/word.run",
            "gil://sentence/3/word",
            "gil://sentence/3/Word.Run",
            "gil://sentence/3/word.run%2F..",
            "gil://sentence/3/../settings",
            "gil://sentences/3/word.run",
            "https://sentence/3/word.run"
        ]
    )
    fun parse_anythingElse_isNull(uri: String) {
        assertThat(SentenceLink.parse(uri)).isNull()
    }
}
