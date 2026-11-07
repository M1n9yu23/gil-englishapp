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
package com.gyugle.gil.feature.progress.impl.data.repository

import androidx.room.withTransaction
import com.gyugle.gil.core.database.databaseCall
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.Mistake
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.impl.data.ProgressDatabase
import com.gyugle.gil.feature.progress.impl.data.entity.LessonCompletionEntity
import com.gyugle.gil.feature.progress.impl.data.entity.SavedItemEntity
import com.gyugle.gil.feature.progress.impl.data.mapper.toCardState
import com.gyugle.gil.feature.progress.impl.data.mapper.toEntity
import com.gyugle.gil.feature.progress.impl.data.mapper.toLog
import com.gyugle.gil.feature.progress.impl.data.mapper.toReviewCard
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler
import com.gyugle.gil.feature.progress.impl.domain.MIN_STUDY_TIME_PER_DAY
import com.gyugle.gil.feature.progress.impl.domain.rate
import com.gyugle.gil.feature.progress.impl.domain.studyStreak
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class RoomProgressRepository @Inject constructor(
    private val database: ProgressDatabase,
    private val clock: StudyClock,
    private val scheduler: FsrsScheduler
) : ProgressObserver,
    ProgressCommands {
    private val cards = database.cardDao()
    private val reviewLogs = database.reviewLogDao()
    private val lessons = database.lessonCompletionDao()
    private val studyTime = database.studyTimeDao()
    private val savedItems = database.savedItemDao()
    private val mistakes = database.mistakeDao()

    override fun observeCard(itemId: StudyItemId): Flow<CardState?> =
        cards.observe(itemId.value).map { it?.toCardState() }.distinctUntilChanged()

    override fun observeCards(): Flow<List<CardState>> =
        cards.observeAll().map { rows -> rows.mapNotNull { it.toCardState() } }.distinctUntilChanged()

    override fun observeDueCards(day: StudyDay): Flow<List<CardState>> =
        cards.observeDue(day.epochDay).map { rows -> rows.mapNotNull { it.toCardState() } }.distinctUntilChanged()

    override fun observeCompletedLessons(): Flow<Set<LessonId>> =
        lessons.observeLessonIds().map { ids -> ids.mapTo(mutableSetOf(), ::LessonId) }.distinctUntilChanged()

    override fun observeStudyTime(from: StudyDay, to: StudyDay): Flow<List<DailyStudyTime>> =
        studyTime.observeRange(from.epochDay, to.epochDay)
            .map { rows -> rows.map { DailyStudyTime(StudyDay(it.studyDay), it.durationMillis.milliseconds) } }
            .distinctUntilChanged()

    override fun observeStreak(today: StudyDay): Flow<Int> =
        studyTime.observeDaysWithAtLeast(MIN_STUDY_TIME_PER_DAY.inWholeMilliseconds)
            .map { days -> studyStreak(days.mapTo(mutableSetOf(), ::StudyDay), today) }
            .distinctUntilChanged()

    override fun observeSavedItems(): Flow<List<StudyItemId>> =
        savedItems.observeItemIds().map { ids -> ids.mapNotNull(StudyItemId::parse) }.distinctUntilChanged()

    override fun observeMistakes(): Flow<List<Mistake>> = mistakes.observeAll()
        .map { rows ->
            rows.mapNotNull { row ->
                StudyItemId.parse(row.itemId)?.let { Mistake(it, row.count, StudyDay(row.lastDay)) }
            }
        }
        .distinctUntilChanged()

    override suspend fun completeLesson(
        lessonId: LessonId,
        introduced: Map<StudyItemId, ReviewRating>
    ): EmptyOutcome<DataError.Local> {
        val now = clock.now()
        val today = clock.dayOf(now)
        return databaseCall {
            database.withTransaction {
                lessons.insert(
                    LessonCompletionEntity(
                        lessonId = lessonId.value,
                        completedAt = now.millis,
                        studyDay = today.epochDay
                    )
                )
                for ((itemId, rating) in introduced) {
                    if (cards.get(itemId.value) != null) continue
                    val card = scheduler.rate(card = null, itemId = itemId, rating = rating, today = today)
                    cards.upsert(card.toEntity(lastReviewAt = now, introducedAt = now))
                    reviewLogs.insert(card.toLog(rating, now))
                }
            }
        }
    }

    override suspend fun recordReview(itemId: StudyItemId, rating: ReviewRating): Outcome<CardState, DataError.Local> {
        val now = clock.now()
        val today = clock.dayOf(now)
        return databaseCall {
            database.withTransaction {
                val stored = cards.get(itemId.value)
                val card = scheduler.rate(
                    card = stored?.toReviewCard(),
                    itemId = itemId,
                    rating = rating,
                    today = today
                )
                val introducedAt = stored?.introducedAt?.let(Instant::fromEpochMilliseconds) ?: now
                cards.upsert(card.toEntity(lastReviewAt = now, introducedAt = introducedAt))
                reviewLogs.insert(card.toLog(rating, now))
                card.toCardState()
            }
        }
    }

    override suspend fun recordStudyTime(duration: Duration): EmptyOutcome<DataError.Local> {
        val millis = duration.inWholeMilliseconds
        if (millis <= 0) return Outcome.Success(Unit)
        val day = clock.today().epochDay
        return databaseCall {
            database.withTransaction {
                studyTime.insertDayIfMissing(day)
                studyTime.addToDay(day, millis)
            }
        }
    }

    override suspend fun setSaved(itemId: StudyItemId, saved: Boolean): EmptyOutcome<DataError.Local> {
        val now = clock.now()
        return databaseCall {
            if (saved) {
                savedItems.insertIfMissing(SavedItemEntity(itemId = itemId.value, savedAt = now.millis))
            } else {
                savedItems.delete(itemId.value)
            }
        }
    }

    override suspend fun recordMistake(itemId: StudyItemId): EmptyOutcome<DataError.Local> {
        val now = clock.now()
        val day = clock.dayOf(now).epochDay
        return databaseCall {
            database.withTransaction {
                mistakes.insertIfMissing(itemId.value, now.millis, day)
                mistakes.addMistake(itemId.value, now.millis, day)
            }
        }
    }

    private val Instant.millis: Long get() = toEpochMilliseconds()
}
