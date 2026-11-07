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
package com.gyugle.gil.feature.dictionary.impl.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.dictionary.impl.domain.model.MistakeEntry
import com.gyugle.gil.feature.progress.api.Mistake
import com.gyugle.gil.feature.progress.api.StudyDay
import org.junit.jupiter.api.Test

class MistakeEntriesTest {
    private val hi = LessonItem(PatternId("pattern.hi"), "Hi.", null, "안녕하세요")
    private val f = LessonItem(SoundId("sound.f"), "f, ph", "f", null)

    @Test
    fun mistakeEntries_keepsTheMistakeOrderAndDropsItemsTheContentLost() {
        val mistakes = listOf(
            Mistake(SoundId("sound.f"), count = 3, lastDay = StudyDay(20)),
            Mistake(SenseId("sense.gone.v.x"), count = 1, lastDay = StudyDay(19)),
            Mistake(PatternId("pattern.hi"), count = 1, lastDay = StudyDay(18))
        )

        assertThat(mistakeEntries(mistakes, items = listOf(hi, f)))
            .containsExactly(MistakeEntry(f, count = 3), MistakeEntry(hi, count = 1))
    }
}
