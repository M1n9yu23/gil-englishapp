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
package com.gyugle.gil.feature.progress.impl.data.mapper

import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.impl.data.entity.CardEntity
import com.gyugle.gil.feature.progress.impl.data.entity.ReviewLogEntity
import com.gyugle.gil.feature.progress.impl.domain.model.MemoryState
import com.gyugle.gil.feature.progress.impl.domain.model.ReviewCard
import kotlin.time.Instant

/** Null for an ID that is no longer a study item; such a row is skipped, not shown. */
fun CardEntity.toReviewCard(): ReviewCard? = StudyItemId.parse(itemId)?.let { id ->
    ReviewCard(
        itemId = id,
        memory = MemoryState(stability, difficulty),
        dueDay = StudyDay(dueDay),
        lastReviewDay = StudyDay(lastReviewDay),
        reviewCount = reviewCount,
        lapseCount = lapseCount
    )
}

fun CardEntity.toCardState(): CardState? = toReviewCard()?.toCardState()

fun ReviewCard.toCardState(): CardState = CardState(
    itemId = itemId,
    stabilityDays = memory.stability,
    dueDay = dueDay,
    lastReviewDay = lastReviewDay,
    reviewCount = reviewCount,
    lapseCount = lapseCount
)

fun ReviewCard.toEntity(lastReviewAt: Instant, introducedAt: Instant): CardEntity = CardEntity(
    itemId = itemId.value,
    stability = memory.stability,
    difficulty = memory.difficulty,
    dueDay = dueDay.epochDay,
    lastReviewDay = lastReviewDay.epochDay,
    lastReviewAt = lastReviewAt.toEpochMilliseconds(),
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    introducedAt = introducedAt.toEpochMilliseconds()
)

/** Logs the rating that produced this card. */
fun ReviewCard.toLog(rating: ReviewRating, reviewedAt: Instant): ReviewLogEntity = ReviewLogEntity(
    itemId = itemId.value,
    rating = rating.storedValue,
    reviewedAt = reviewedAt.toEpochMilliseconds(),
    studyDay = lastReviewDay.epochDay,
    stability = memory.stability,
    difficulty = memory.difficulty,
    dueDay = dueDay.epochDay
)

/** The FSRS grade number, so the log reads the same as other FSRS tools. */
val ReviewRating.storedValue: Int
    get() = when (this) {
        ReviewRating.AGAIN -> 1
        ReviewRating.HARD -> 2
        ReviewRating.GOOD -> 3
    }

fun reviewRatingOf(storedValue: Int): ReviewRating? = ReviewRating.entries.firstOrNull { it.storedValue == storedValue }
