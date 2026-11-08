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
package com.gyugle.gil.feature.settings.impl.domain.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class AppLanguageTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(
        "'', SYSTEM",
        "ko, KOREAN",
        "ko-KR, KOREAN",
        "en, ENGLISH",
        "en-GB, ENGLISH",
        "EN-us, ENGLISH",
        "'en-US,ko', ENGLISH",
        "'ko,en', KOREAN",
        "ja, SYSTEM",
        "'ja,ko', SYSTEM"
    )
    fun fromTags_readsTheFirstLanguageOnly(tags: String, expected: AppLanguage) {
        assertThat(AppLanguage.fromTags(tags)).isEqualTo(expected)
    }
}
