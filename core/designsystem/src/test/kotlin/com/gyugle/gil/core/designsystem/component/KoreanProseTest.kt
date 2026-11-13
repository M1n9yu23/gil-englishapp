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
package com.gyugle.gil.core.designsystem.component

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class KoreanProseTest {
    @Test
    fun proseOf_particleAfterEnglish_isJoinedToIt() {
        assertThat(proseOf("`Hi.`도 인사예요.").text).isEqualTo("Hi.⁠도 인사예요.")
    }

    @Test
    fun proseOf_quoteAroundEnglish_isJoinedOnBothSides() {
        assertThat(proseOf("\"`f`\" 소리").text).isEqualTo("\"⁠f⁠\" 소리")
    }

    @Test
    fun proseOf_spaceAroundEnglish_addsNothing() {
        assertThat(proseOf("방금 들은 `Hello.` 기억나요?").text).isEqualTo("방금 들은 Hello. 기억나요?")
    }

    @Test
    fun proseOf_englishParts_areTheSpannedRanges() {
        val prose = proseOf("`Hello.`와 `Hi.`는")

        assertThat(prose.spanStyles.map { prose.text.substring(it.start, it.end) }).containsExactly("Hello.", "Hi.")
    }

    @Test
    fun proseOf_apostropheInEnglish_isTypographic() {
        assertThat(proseOf("`I'm`은 `I am`이에요.").text).isEqualTo("I’m⁠은 I am⁠이에요.")
    }

    @Test
    fun proseOf_unmatchedBacktick_keepsTheText() {
        assertThat(proseOf("`Hi.는").text).isEqualTo("`Hi.는")
    }
}
