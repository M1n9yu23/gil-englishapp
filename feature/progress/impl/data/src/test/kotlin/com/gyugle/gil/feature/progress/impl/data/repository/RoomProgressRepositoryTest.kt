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

import android.app.Application
import androidx.room.Room
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.Mistake
import com.gyugle.gil.feature.progress.api.ReviewRating
import com.gyugle.gil.feature.progress.impl.data.ProgressDatabase
import com.gyugle.gil.feature.progress.impl.data.mapper.toCardState
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler
import com.gyugle.gil.feature.progress.impl.domain.rate
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomProgressRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        ProgressDatabase::class.java
    ).build()

    // 12:00 in Seoul.
    private val clock = FakeStudyClock(now = Instant.parse("2026-11-07T03:00:00Z"), zone = TimeZone.of("Asia/Seoul"))
    private val scheduler = FsrsScheduler()
    private val repository = RoomProgressRepository(database, clock, scheduler)

    private val hello = SenseId("sense.hello.interj.greeting")
    private val bye = SenseId("sense.bye.interj.leaving")
    private val letterA = SoundId("sound.letter-a")
    private val howAreYou = PatternId("pattern.how-are-you")

    @After
    fun close() = database.close()

    @Test
    fun completeLesson_newItems_startsOneCardPerItemFromItsRating() = runTest {
        val today = clock.today()

        repository.completeLesson(
            LessonId("lesson.s1-15.1"),
            mapOf(
                hello to ReviewRating.GOOD,
                letterA to ReviewRating.AGAIN
            )
        )

        assertThat(repository.observeCards().first()).containsExactly(
            scheduler.rate(null, hello, ReviewRating.GOOD, today).toCardState(),
            scheduler.rate(null, letterA, ReviewRating.AGAIN, today).toCardState()
        )
        assertThat(repository.observeCompletedLessons().first()).isEqualTo(setOf(LessonId("lesson.s1-15.1")))
    }

    @Test
    fun completeLesson_again_keepsExistingScheduleAndAddsNewItems() = runTest {
        repository.completeLesson(LessonId("lesson.s1-15.1"), mapOf(hello to ReviewRating.GOOD))
        val before = repository.observeCard(hello).first()
        clock.advanceBy(1.days)

        repository.completeLesson(
            LessonId("lesson.s1-15.1"),
            mapOf(
                hello to ReviewRating.AGAIN,
                bye to ReviewRating.GOOD
            )
        )

        assertThat(repository.observeCard(hello).first()).isEqualTo(before)
        assertThat(repository.observeCard(bye).first()?.lastReviewDay).isEqualTo(clock.today())
        assertThat(repository.observeCompletedLessons().first()).isEqualTo(setOf(LessonId("lesson.s1-15.1")))
    }

    @Test
    fun recordReview_existingCard_reschedulesFromStudyDaysSinceLastReview() = runTest {
        val firstDay = clock.today()
        repository.completeLesson(LessonId("lesson.s1-15.1"), mapOf(hello to ReviewRating.GOOD))
        clock.advanceBy(3.days)

        val result = repository.recordReview(hello, ReviewRating.HARD)

        val first = scheduler.rate(null, hello, ReviewRating.GOOD, firstDay)
        val expected = scheduler.rate(first, hello, ReviewRating.HARD, clock.today()).toCardState()
        assertThat(result).isEqualTo(Outcome.Success(expected))
        assertThat(repository.observeCard(hello).first()).isEqualTo(expected)
    }

    @Test
    fun recordReview_itemWithoutCard_startsOne() = runTest {
        val result = repository.recordReview(howAreYou, ReviewRating.GOOD)

        assertThat(result).isInstanceOf(Outcome.Success::class)
        assertThat(repository.observeCard(howAreYou).first()?.reviewCount).isEqualTo(1)
    }

    @Test
    fun observeCard_unknownItem_isNull() = runTest {
        assertThat(repository.observeCard(hello).first()).isNull()
    }

    @Test
    fun observeDueCards_returnsCardsDueByThatDay_longestOverdueFirst() = runTest {
        // Again is due in one day, good in three.
        repository.completeLesson(LessonId("lesson.s1-15.1"), mapOf(hello to ReviewRating.GOOD))
        clock.advanceBy(1.days)
        repository.completeLesson(LessonId("lesson.s1-15.2"), mapOf(bye to ReviewRating.AGAIN))
        val today = clock.today()

        assertThat(repository.observeDueCards(today).first()).isEqualTo(emptyList())
        assertThat(repository.observeDueCards(today + 2).first().map { it.itemId }).containsExactly(bye, hello)

        repository.observeDueCards(today + 1).test {
            assertThat(awaitItem().map { it.itemId }).containsExactly(bye)
            // Brings hello forward to the same day; it was introduced first, so it comes first.
            repository.recordReview(hello, ReviewRating.AGAIN)
            assertThat(awaitItem().map { it.itemId }).containsExactly(hello, bye)
        }
    }

    @Test
    fun recordStudyTime_addsUpWithinTheStudyDay() = runTest {
        repository.recordStudyTime(4.minutes)
        // 02:00 the next morning in Seoul still belongs to the same study day.
        clock.advanceBy(14.hours)
        repository.recordStudyTime(90.seconds)
        repository.recordStudyTime(0.seconds)
        repository.recordStudyTime((-5).seconds)
        val day = clock.today()

        assertThat(repository.observeStudyTime(day - 3, day).first()).containsExactly(
            DailyStudyTime(day, 4.minutes + 90.seconds)
        )
    }

    @Test
    fun observeStudyTime_onlyDaysInRange() = runTest {
        val first = clock.today()
        repository.recordStudyTime(1.minutes)
        clock.advanceBy(1.days)
        repository.recordStudyTime(2.minutes)
        clock.advanceBy(1.days)
        repository.recordStudyTime(3.minutes)

        assertThat(repository.observeStudyTime(first + 1, first + 2).first()).containsExactly(
            DailyStudyTime(first + 1, 2.minutes),
            DailyStudyTime(first + 2, 3.minutes)
        )
    }

    @Test
    fun observeStreak_daysUnderFiveMinutesAreDaysOff() = runTest {
        repository.recordStudyTime(5.minutes)
        clock.advanceBy(1.days)
        repository.recordStudyTime(4.minutes + 59.seconds)
        clock.advanceBy(1.days)

        repository.observeStreak(clock.today()).test {
            assertThat(awaitItem()).isEqualTo(1)
            repository.recordStudyTime(5.minutes)
            assertThat(awaitItem()).isEqualTo(2)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun setSaved_savingTwice_keepsTheFirstSave() = runTest {
        repository.setSaved(hello, saved = true)
        clock.advanceBy(1.minutes)
        repository.setSaved(bye, saved = true)
        clock.advanceBy(1.minutes)
        repository.setSaved(hello, saved = true)

        assertThat(repository.observeSavedItems().first()).containsExactly(bye, hello)
    }

    @Test
    fun setSaved_false_removesTheItem() = runTest {
        repository.setSaved(hello, saved = true)

        repository.setSaved(hello, saved = false)
        repository.setSaved(hello, saved = false)

        assertThat(repository.observeSavedItems().first()).isEqualTo(emptyList())
    }

    @Test
    fun recordMistake_countsAndKeepsTheLatestDay_mostRecentFirst() = runTest {
        repository.recordMistake(hello)
        clock.advanceBy(1.days)
        repository.recordMistake(bye)
        clock.advanceBy(1.days)
        repository.recordMistake(hello)

        assertThat(repository.observeMistakes().first()).containsExactly(
            Mistake(hello, count = 2, lastDay = clock.today()),
            Mistake(bye, count = 1, lastDay = clock.today() - 1)
        )
    }

    @Test
    fun clearMistake_removesOnlyThatItem() = runTest {
        repository.recordMistake(hello)
        repository.recordMistake(bye)

        repository.clearMistake(hello)
        repository.clearMistake(hello)

        assertThat(
            repository.observeMistakes().first()
        ).containsExactly(Mistake(bye, count = 1, lastDay = clock.today()))
    }
}
