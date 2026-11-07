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
package com.gyugle.gil.core.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ContentReportUrlTest {
    @Test
    fun contentReportUrl_korean_picksTheKoreanFormAndEncodesEveryField() {
        val url = contentReportUrl(
            location = "word.zero",
            appVersion = "0.1.0-debug",
            shown = "Zero, one & two?",
            korean = true
        )

        assertThat(url).isEqualTo(
            "https://github.com/M1n9yu23/gil-englishapp/issues/new?template=content_error.ko.yml" +
                "&location=word.zero&app-version=0.1.0-debug&shown=Zero%2C+one+%26+two%3F"
        )
    }

    @Test
    fun contentReportUrl_englishWithoutShownText_leavesThatFieldOut() {
        val url = contentReportUrl(location = "sound.f", appVersion = "0.1.0", shown = null, korean = false)

        assertThat(url).isEqualTo(
            "https://github.com/M1n9yu23/gil-englishapp/issues/new?template=content_error.yml" +
                "&location=sound.f&app-version=0.1.0"
        )
    }

    @Test
    fun contentReportUrl_koreanText_isPercentEncodedAsUtf8() {
        val url = contentReportUrl(location = "word.zero", appVersion = "1", shown = "영", korean = true)

        assertThat(url.substringAfter("shown=")).isEqualTo("%EC%98%81")
    }
}
