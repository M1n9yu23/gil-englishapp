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
package com.gyugle.gil.feature.progress.impl.domain

import assertk.assertThat
import assertk.assertions.isCloseTo
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.impl.domain.model.MemoryState
import com.gyugle.gil.feature.progress.impl.domain.model.ReviewCard
import org.junit.jupiter.api.Test

class CardRatingTest {
    private val scheduler = FsrsScheduler()
    private val item = SenseId("sense.cat.n.animal")
    private val today = StudyDay(1_000)

    @Test
    fun rate_newItemGood_startsCardDueAfterFirstInterval() {
        val card = scheduler.rate(card = null, itemId = item, rating = ReviewRating.GOOD, today = today)

        // Default weights give again 1 day and hard 1 day, raised to 2; good (2.3 days) is raised to 3.
        assertThat(card.dueDay).isEqualTo(today + 3)
        assertThat(card.lastReviewDay).isEqualTo(today)
        assertThat(card.reviewCount).isEqualTo(1)
        assertThat(card.lapseCount).isEqualTo(0)
    }

    @Test
    fun rate_newItemAgain_isNotALapse() {
        val card = scheduler.rate(card = null, itemId = item, rating = ReviewRating.AGAIN, today = today)

        assertThat(card.lapseCount).isEqualTo(0)
        assertThat(card.dueDay).isEqualTo(today + 1)
    }

    @Test
    fun rate_existingCardAgain_countsALapse() {
        val card = scheduler.rate(existing(lastReviewDay = today - 5), item, ReviewRating.AGAIN, today)

        assertThat(card.lapseCount).isEqualTo(3)
        assertThat(card.reviewCount).isEqualTo(5)
    }

    @Test
    fun rate_existingCard_usesStudyDaysSinceLastReview() {
        val existing = existing(lastReviewDay = today - 5)

        val card = scheduler.rate(existing, item, ReviewRating.GOOD, today)

        val expected = scheduler.review(existing.memory, elapsedDays = 5, grade = FsrsScheduler.Grade.GOOD)
        assertThat(card.memory).isEqualTo(expected.memory)
        assertThat(card.dueDay).isEqualTo(today + expected.intervalDays)
    }

    @Test
    fun rate_clockMovedBackPastLastReview_treatsAsNoTimePassed() {
        val existing = existing(lastReviewDay = today + 2)

        val card = scheduler.rate(existing, item, ReviewRating.GOOD, today)

        assertThat(card.memory.stability).isCloseTo(existing.memory.stability, 1e-9)
        assertThat(card.lastReviewDay).isEqualTo(today)
    }

    private fun existing(lastReviewDay: StudyDay) = ReviewCard(
        itemId = item,
        memory = MemoryState(stability = 4.0, difficulty = 5.0),
        dueDay = lastReviewDay + 4,
        lastReviewDay = lastReviewDay,
        reviewCount = 4,
        lapseCount = 2
    )
}
