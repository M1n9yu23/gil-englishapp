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

class StudyItemIdTest {
    @Test
    fun parse_eachStudyItemPrefix_returnsThatKind() {
        assertThat(StudyItemId.parse("sense.run.v.move-fast")).isEqualTo(SenseId("sense.run.v.move-fast"))
        assertThat(StudyItemId.parse("pattern.how-are-you")).isEqualTo(PatternId("pattern.how-are-you"))
        assertThat(StudyItemId.parse("sound.letter-a")).isEqualTo(SoundId("sound.letter-a"))
    }

    @Test
    fun parse_otherContentIds_returnsNull() {
        assertThat(StudyItemId.parse("word.run")).isNull()
        assertThat(StudyItemId.parse("lesson.s1-01.1")).isNull()
        assertThat(StudyItemId.parse("")).isNull()
    }

    @Test
    fun constructor_idOfAnotherKind_isRejected() {
        assertFailure { SenseId("pattern.how-are-you") }.isInstanceOf(IllegalArgumentException::class)
        assertFailure { PatternId("sound.letter-a") }.isInstanceOf(IllegalArgumentException::class)
        assertFailure { SoundId("word.run") }.isInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun parse_prefixWithoutName_returnsNull() {
        assertThat(StudyItemId.parse("sense.")).isNull()
        assertThat(StudyItemId.parse("sense")).isNull()
    }
}
