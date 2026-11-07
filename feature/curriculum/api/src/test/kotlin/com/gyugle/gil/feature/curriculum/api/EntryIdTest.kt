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
package com.gyugle.gil.feature.curriculum.api

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class EntryIdTest {
    @Test
    fun parse_eachEntryPrefix_returnsThatKind() {
        assertThat(EntryId.parse("word.run")).isEqualTo(WordId("word.run"))
        assertThat(EntryId.parse("pattern.how-are-you")).isEqualTo(PatternId("pattern.how-are-you"))
        assertThat(EntryId.parse("sound.letter-a")).isEqualTo(SoundId("sound.letter-a"))
    }

    @Test
    fun parse_idsWithoutAPage_returnsNull() {
        assertThat(EntryId.parse("sense.run.v.move-fast")).isNull()
        assertThat(EntryId.parse("lesson.s1-01.1")).isNull()
        assertThat(EntryId.parse("word.")).isNull()
        assertThat(EntryId.parse("")).isNull()
    }

    @Test
    fun wordId_otherKindOfId_isRejected() {
        assertFailure { WordId("sense.run.v.move-fast") }.isInstanceOf(IllegalArgumentException::class)
        assertFailure { WordId("word.") }.isInstanceOf(IllegalArgumentException::class)
    }
}
