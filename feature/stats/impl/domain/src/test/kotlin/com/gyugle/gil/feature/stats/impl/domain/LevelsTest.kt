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
package com.gyugle.gil.feature.stats.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.stats.impl.domain.model.ItemLevel
import com.gyugle.gil.feature.stats.impl.domain.model.LevelCounts
import org.junit.jupiter.api.Test

class LevelsTest {
    private fun card(stabilityDays: Double) = CardState(
        SenseId("sense.hi.interj.greeting"),
        stabilityDays = stabilityDays,
        dueDay = StudyDay(10),
        lastReviewDay = StudyDay(0),
        reviewCount = 1,
        lapseCount = 0
    )

    @Test
    fun level_belowAWeek_isLearning() {
        assertThat(card(6.9).level()).isEqualTo(ItemLevel.LEARNING)
    }

    @Test
    fun level_aWeekToThreeWeeks_isFamiliar() {
        assertThat(card(7.0).level()).isEqualTo(ItemLevel.FAMILIAR)
        assertThat(card(20.9).level()).isEqualTo(ItemLevel.FAMILIAR)
    }

    @Test
    fun level_threeWeeksOrMore_isLasting() {
        assertThat(card(21.0).level()).isEqualTo(ItemLevel.LASTING)
    }

    @Test
    fun countLevels_mixedCards_countsEachLevel() {
        val counts = countLevels(listOf(card(1.0), card(3.0), card(10.0), card(40.0)))

        assertThat(counts).isEqualTo(LevelCounts(learning = 2, familiar = 1, lasting = 1))
        assertThat(counts.total).isEqualTo(4)
    }

    @Test
    fun countLevels_noCards_allZero() {
        assertThat(countLevels(emptyList())).isEqualTo(LevelCounts(0, 0, 0))
    }
}
