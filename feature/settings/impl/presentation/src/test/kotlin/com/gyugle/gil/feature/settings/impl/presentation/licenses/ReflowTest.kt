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
package com.gyugle.gil.feature.settings.impl.presentation.licenses

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class ReflowTest {
    @Test
    fun reflow_joinsWrappedLinesAndKeepsParagraphs() {
        val text = "                                 Apache License\r\n" +
            "                           Version 2.0, January 2004\r\n" +
            "\r\n" +
            "   1. Definitions.\n" +
            "      \"License\" shall mean the terms\n" +
            "      and conditions.\n" +
            "\n\n\n"

        assertThat(reflow(text)).isEqualTo(
            "Apache License Version 2.0, January 2004\n\n1. Definitions. \"License\" shall mean the terms and conditions."
        )
    }

    @Test
    fun reflow_shortCapitalHeading_keepsItsOwnLine() {
        val text = "PREAMBLE\nThe goals of the Open Font License (OFL) are\nto stimulate development.\n"

        assertThat(reflow(text)).isEqualTo(
            "PREAMBLE\nThe goals of the Open Font License (OFL) are to stimulate development."
        )
    }

    @Test
    fun reflow_capitalizedParagraph_isJoined() {
        val text = "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS\n\"AS IS\" AND ANY EXPRESS\n"

        assertThat(reflow(text)).isEqualTo(
            "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \"AS IS\" AND ANY EXPRESS"
        )
    }
}
