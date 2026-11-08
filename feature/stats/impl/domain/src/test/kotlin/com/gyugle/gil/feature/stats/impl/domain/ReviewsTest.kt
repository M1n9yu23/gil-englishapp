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
import org.junit.jupiter.api.Test

class ReviewsTest {
    private val today = StudyDay(100)

    private fun card(due: Int) = CardState(
        SenseId("sense.hi.interj.greeting"),
        stabilityDays = 3.0,
        dueDay = today + due,
        lastReviewDay = today - 3,
        reviewCount = 1,
        lapseCount = 0
    )

    @Test
    fun reviewForecast_overdueCards_countForToday() {
        val forecast = reviewForecast(listOf(card(-5), card(-1), card(0)), today)

        assertThat(forecast).isEqualTo(listOf(3, 0, 0, 0, 0, 0, 0))
    }

    @Test
    fun reviewForecast_cardsAhead_countOnTheirDayAndStopAfterAWeek() {
        val forecast = reviewForecast(listOf(card(1), card(1), card(6), card(7), card(30)), today)

        assertThat(forecast).isEqualTo(listOf(0, 2, 0, 0, 0, 0, 1))
    }
}
