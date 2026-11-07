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
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class DictionaryRouteTest {
    @Test
    fun serialNames_stayFixed_soSavedBackStacksStillRestore() {
        assertThat(Json.encodeToString<DictionaryRoute>(WordsRoute)).isEqualTo("""{"type":"dictionary.home"}""")
        assertThat(Json.encodeToString<DictionaryRoute>(EntryRoute("word.run")))
            .isEqualTo("""{"type":"dictionary.entry","entryId":"word.run"}""")
        assertThat(Json.encodeToString<DictionaryRoute>(SentenceRoute(3, "word.run")))
            .isEqualTo("""{"type":"dictionary.sentence","sentenceId":3,"source":"word.run"}""")
    }
}
