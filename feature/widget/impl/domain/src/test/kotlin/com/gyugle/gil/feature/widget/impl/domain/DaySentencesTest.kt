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
package com.gyugle.gil.feature.widget.impl.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay
import org.junit.jupiter.api.Test

class DaySentencesTest {
    private val today = StudyDay(20_000)

    private fun card(id: StudyItemId, lastReview: StudyDay, reviews: Int) = CardState(
        id,
        stabilityDays = 3.0,
        dueDay = today + 3,
        lastReviewDay = lastReview,
        reviewCount = reviews,
        lapseCount = 0
    )

    @Test
    fun sentenceItems_keepsWordsAndPatternsMetBeforeToday_evenWhenReviewedToday() {
        val word = SenseId("sense.run.v.move-fast")
        val pattern = PatternId("pattern.how-are-you")
        val reviewedToday = SenseId("sense.walk.v.move")

        val items = sentenceItems(
            today,
            listOf(card(word, today - 2, 1), card(pattern, today - 1, 3), card(reviewedToday, today, 2))
        )

        assertThat(items).containsExactly(word, pattern, reviewedToday)
    }

    @Test
    fun sentenceItems_leavesOutItemsFirstMetTodayAndSounds() {
        val items = sentenceItems(
            today,
            listOf(
                card(SenseId("sense.run.v.move-fast"), today, 1),
                card(SoundId("sound.letter-a"), today - 5, 4)
            )
        )

        assertThat(items).isEmpty()
    }

    @Test
    fun dayOrder_isTheSameWhateverOrderTheItemsCameIn() {
        val items = (1..10).map { SenseId("sense.word$it.n.x") }

        assertThat(dayOrder(today, items.reversed())).isEqualTo(dayOrder(today, items))
    }

    @Test
    fun dayOrder_changesTheNextDay() {
        val items = (1..10).map { SenseId("sense.word$it.n.x") }

        assertThat(dayOrder(today + 1, items)).isNotEqualTo(dayOrder(today, items))
    }

    @Test
    fun exampleIndex_movesToTheNextExampleEachDay_andWrapsAround() {
        assertThat((0..3).map { exampleIndex(StudyDay(9L + it), 3) }).containsExactly(0, 1, 2, 0)
    }
}
