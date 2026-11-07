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
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.impl.data.ProgressTables
import com.gyugle.gil.feature.progress.impl.data.dto.CardDto
import com.gyugle.gil.feature.progress.impl.data.dto.LessonCompletionDto
import com.gyugle.gil.feature.progress.impl.data.dto.MistakeDto
import com.gyugle.gil.feature.progress.impl.data.dto.ProgressBackupDto
import com.gyugle.gil.feature.progress.impl.data.dto.RatingDto
import com.gyugle.gil.feature.progress.impl.data.dto.ReviewDto
import com.gyugle.gil.feature.progress.impl.data.dto.SavedItemDto
import com.gyugle.gil.feature.progress.impl.data.dto.StudyTimeDto
import com.gyugle.gil.feature.progress.impl.data.entity.CardEntity
import com.gyugle.gil.feature.progress.impl.data.entity.LessonCompletionEntity
import com.gyugle.gil.feature.progress.impl.data.entity.MistakeEntity
import com.gyugle.gil.feature.progress.impl.data.entity.ReviewLogEntity
import com.gyugle.gil.feature.progress.impl.data.entity.SavedItemEntity
import com.gyugle.gil.feature.progress.impl.data.entity.StudyTimeEntity
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

/**
 * The record as a backup section. Rows a restore would reject are left out, so a file the app
 * wrote can always be read back; the screens skip the same rows.
 */
fun ProgressTables.toBackupDto(): ProgressBackupDto = ProgressBackupDto(
    version = ProgressBackupDto.VERSION,
    cards = cards.map {
        CardDto(
            item = it.itemId,
            stability = it.stability,
            difficulty = it.difficulty,
            dueDay = it.dueDay.toDate(),
            lastReviewDay = it.lastReviewDay.toDate(),
            lastReviewAt = it.lastReviewAt.toInstant(),
            reviewCount = it.reviewCount,
            lapseCount = it.lapseCount,
            introducedAt = it.introducedAt.toInstant()
        )
    }.filter { it.isValid() },
    reviews = reviews.mapNotNull { log ->
        val rating = reviewRatingOf(log.rating) ?: return@mapNotNull null
        ReviewDto(
            item = log.itemId,
            rating = rating.toDto(),
            reviewedAt = log.reviewedAt.toInstant(),
            day = log.studyDay.toDate(),
            stability = log.stability,
            difficulty = log.difficulty,
            dueDay = log.dueDay.toDate()
        )
    }.filter { it.isValid() },
    lessons = lessons.map { LessonCompletionDto(it.lessonId, it.completedAt.toInstant(), it.studyDay.toDate()) }
        .filter { it.isValid() },
    studyTime = studyTime.map { StudyTimeDto(it.studyDay.toDate(), it.durationMillis) }.filter { it.isValid() },
    saved = saved.map { SavedItemDto(it.itemId, it.savedAt.toInstant()) }.filter { it.isValid() },
    mistakes = mistakes.map { MistakeDto(it.itemId, it.count, it.lastAt.toInstant(), it.lastDay.toDate()) }
        .filter { it.isValid() }
)

/**
 * The tables to restore, or null when any row is out of range or repeated. A backup file comes
 * from outside the app, so nothing in it is trusted.
 */
fun ProgressBackupDto.toTablesOrNull(): ProgressTables? {
    val valid = cards.all { it.isValid() } &&
        reviews.all { it.isValid() } &&
        lessons.all { it.isValid() } &&
        studyTime.all { it.isValid() } &&
        saved.all { it.isValid() } &&
        mistakes.all { it.isValid() } &&
        cards.map { it.item }.isDistinct() &&
        studyTime.map { it.day }.isDistinct() &&
        saved.map { it.item }.isDistinct() &&
        mistakes.map { it.item }.isDistinct()
    if (!valid) return null
    return ProgressTables(
        cards = cards.map {
            CardEntity(
                itemId = it.item,
                stability = it.stability,
                difficulty = it.difficulty,
                dueDay = it.dueDay.toEpochDays(),
                lastReviewDay = it.lastReviewDay.toEpochDays(),
                lastReviewAt = it.lastReviewAt.toEpochMilliseconds(),
                reviewCount = it.reviewCount,
                lapseCount = it.lapseCount,
                introducedAt = it.introducedAt.toEpochMilliseconds()
            )
        },
        reviews = reviews.map {
            ReviewLogEntity(
                itemId = it.item,
                rating = it.rating.toRating().storedValue,
                reviewedAt = it.reviewedAt.toEpochMilliseconds(),
                studyDay = it.day.toEpochDays(),
                stability = it.stability,
                difficulty = it.difficulty,
                dueDay = it.dueDay.toEpochDays()
            )
        },
        lessons = lessons.map {
            LessonCompletionEntity(
                lessonId = it.lesson,
                completedAt = it.completedAt.toEpochMilliseconds(),
                studyDay = it.day.toEpochDays()
            )
        },
        studyTime = studyTime.map { StudyTimeEntity(it.day.toEpochDays(), it.millis) },
        saved = saved.map { SavedItemEntity(it.item, it.savedAt.toEpochMilliseconds()) },
        mistakes = mistakes.map {
            MistakeEntity(it.item, it.count, it.lastAt.toEpochMilliseconds(), it.lastDay.toEpochDays())
        }
    )
}

private const val LESSON_PREFIX = "lesson."
private const val MIN_STABILITY = 0.001
private const val MAX_STABILITY = 36_500.0

/** Far above any real count, low enough that adding one more never overflows an Int. */
private const val MAX_COUNT = 1_000_000

/** A study day is at most 25 hours long, on the night the clocks go back. */
private val MAX_DAY_MILLIS = 25.hours.inWholeMilliseconds

private fun CardDto.isValid() = item.isStudyItem() &&
    isValidMemory(stability, difficulty) &&
    reviewCount in 1..MAX_COUNT &&
    lapseCount in 0 until reviewCount &&
    dueDay >= lastReviewDay

private fun ReviewDto.isValid() = item.isStudyItem() && isValidMemory(stability, difficulty) && dueDay >= day

private fun LessonCompletionDto.isValid() = lesson.length > LESSON_PREFIX.length && lesson.startsWith(LESSON_PREFIX)

private fun StudyTimeDto.isValid() = millis in 0..MAX_DAY_MILLIS

private fun SavedItemDto.isValid() = item.isStudyItem()

private fun MistakeDto.isValid() = item.isStudyItem() && count in 1..MAX_COUNT

private fun isValidMemory(stability: Double, difficulty: Double) =
    stability in MIN_STABILITY..MAX_STABILITY && difficulty in 1.0..10.0

private fun String.isStudyItem() = StudyItemId.parse(this) != null

private fun <T> List<T>.isDistinct() = toSet().size == size

private fun Long.toDate(): LocalDate = LocalDate.fromEpochDays(this)

private fun Long.toInstant(): Instant = Instant.fromEpochMilliseconds(this)

private fun ReviewRating.toDto(): RatingDto = when (this) {
    ReviewRating.AGAIN -> RatingDto.AGAIN
    ReviewRating.HARD -> RatingDto.HARD
    ReviewRating.GOOD -> RatingDto.GOOD
}

private fun RatingDto.toRating(): ReviewRating = when (this) {
    RatingDto.AGAIN -> ReviewRating.AGAIN
    RatingDto.HARD -> ReviewRating.HARD
    RatingDto.GOOD -> ReviewRating.GOOD
}
