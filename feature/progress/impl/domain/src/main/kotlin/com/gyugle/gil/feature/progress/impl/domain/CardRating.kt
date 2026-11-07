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

import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.impl.domain.model.ReviewCard

/**
 * Applies [rating], given on [today], to [card], or starts a card for [itemId] when there is none.
 *
 * Elapsed time counts study days, not hours: a review at 23:00 and the next at 05:00 the next
 * morning are one day apart. If the clock moved back past the last review, no time has passed.
 */
fun FsrsScheduler.rate(card: ReviewCard?, itemId: StudyItemId, rating: ReviewRating, today: StudyDay): ReviewCard {
    require(card == null || card.itemId == itemId) { "The card belongs to another item" }
    val elapsedDays = card?.lastReviewDay?.daysUntil(today)?.coerceIn(0, Int.MAX_VALUE.toLong())?.toInt() ?: 0
    val review = review(card?.memory, elapsedDays, rating.toGrade())
    val lapsed = card != null && rating == ReviewRating.AGAIN
    return ReviewCard(
        itemId = itemId,
        memory = review.memory,
        dueDay = today + review.intervalDays,
        lastReviewDay = today,
        reviewCount = (card?.reviewCount ?: 0) + 1,
        lapseCount = (card?.lapseCount ?: 0) + if (lapsed) 1 else 0
    )
}

private fun ReviewRating.toGrade(): FsrsScheduler.Grade = when (this) {
    ReviewRating.AGAIN -> FsrsScheduler.Grade.AGAIN
    ReviewRating.HARD -> FsrsScheduler.Grade.HARD
    ReviewRating.GOOD -> FsrsScheduler.Grade.GOOD
}
