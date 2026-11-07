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
import assertk.assertions.isLessThan
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler.Grade
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler.Grade.AGAIN
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler.Grade.EASY
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler.Grade.GOOD
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler.Grade.HARD
import com.gyugle.gil.feature.progress.impl.domain.model.MemoryState
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Compares the scheduler with the expected values in the ts-fsrs 6.0.0 test suite
 * (https://github.com/open-spaced-repetition/ts-fsrs, MIT License, Copyright (c) 2023 Open Spaced
 * Repetition), files packages/fsrs/src/legacy/__tests__/algorithm.test.ts, FSRS-6.test.ts and
 * impl/long-term_scheduler.test.ts.
 *
 * ts-fsrs rounds to eight decimal places between steps and this class does not, so stability and
 * difficulty are compared to four places, as ts-fsrs itself does against fsrs-rs.
 */
class FsrsSchedulerTest {
    private val scheduler = FsrsScheduler()

    @Test
    fun retrievability_defaultWeights_matchesForgettingCurve() {
        val expected = listOf(1.0, 0.9, 0.84588465, 0.8093881)

        expected.forEachIndexed { days, r ->
            assertThat(scheduler.retrievability(days.toDouble(), stability = 1.0)).isCloseTo(r, TOLERANCE)
        }
    }

    @Test
    fun nextMemory_newItem_startsFromGradeWeights() {
        val expectedStability = listOf(0.212, 1.2931, 2.3065, 8.2956)
        val expectedDifficulty = listOf(6.4133, 5.11217071, 2.11810397, 1.0)

        Grade.entries.forEachIndexed { i, grade ->
            val memory = scheduler.nextMemory(memory = null, elapsedDays = 0, grade = grade)
            assertThat(memory.stability).isCloseTo(expectedStability[i], TOLERANCE)
            assertThat(memory.difficulty).isCloseTo(expectedDifficulty[i], TOLERANCE)
        }
    }

    @Test
    fun nextMemory_difficultyFive_movesDifficultyByGrade() {
        val expected = listOf(8.34176237, 6.66599536, 4.99022837, 3.31446137)

        Grade.entries.forEachIndexed { i, grade ->
            val memory = scheduler.nextMemory(
                MemoryState(stability = 5.0, difficulty = 5.0),
                elapsedDays = 5,
                grade = grade
            )
            assertThat(memory.difficulty).isCloseTo(expected[i], TOLERANCE)
        }
    }

    @Test
    fun recallAndForgetStability_givenRetrievability_matchReference() {
        val difficulty = listOf(1.0, 2.0, 3.0, 4.0)
        val retrievability = listOf(0.9, 0.8, 0.7, 0.6)
        val expectedRecall = listOf(25.60252118, 28.22657096, 58.65599107, 127.2266925)
        val expectedForget = listOf(1.05253961, 1.18943295, 1.36808387, 1.58498896)

        Grade.entries.forEachIndexed { i, grade ->
            val memory = MemoryState(stability = 5.0, difficulty = difficulty[i])
            assertThat(
                scheduler.recallStability(memory, retrievability[i], grade)
            ).isCloseTo(expectedRecall[i], TOLERANCE)
            assertThat(scheduler.forgetStability(memory, retrievability[i])).isCloseTo(expectedForget[i], TOLERANCE)
        }
    }

    @ParameterizedTest
    @CsvSource("0.2, 34793", "0.3, 2508", "0.4, 387", "0.5, 90", "0.6, 27", "0.7, 9", "0.8, 3", "0.9, 1")
    fun nextInterval_stabilityOne_dependsOnDesiredRetention(retention: Double, days: Int) {
        val scheduler = FsrsScheduler(FsrsParameters(desiredRetention = retention))

        assertThat(scheduler.nextInterval(stability = 1.0)).isEqualTo(days)
    }

    @Test
    fun nextMemory_againThenFiveGoods_matchesLongTermMemoryState() {
        val grades = listOf(AGAIN, GOOD, GOOD, GOOD, GOOD, GOOD)
        val elapsed = listOf(0, 0, 1, 3, 8, 21)

        var memory: MemoryState? = null
        grades.forEachIndexed { i, grade -> memory = scheduler.nextMemory(memory, elapsed[i], grade) }

        val final = checkNotNull(memory)
        assertThat(final.stability).isCloseTo(53.335106, TOLERANCE)
        assertThat(final.difficulty).isCloseTo(6.3574867, TOLERANCE)
    }

    @Test
    fun review_goodsWithTwoLapses_matchesLongTermHistory() = assertHistory(
        grades = listOf(GOOD, GOOD, GOOD, GOOD, GOOD, GOOD, AGAIN, AGAIN, GOOD, GOOD, GOOD, GOOD, GOOD),
        intervals = listOf(3, 13, 48, 155, 445, 1158, 17, 3, 11, 37, 112, 307, 773),
        stability = listOf(
            3.0412, 13.09130698, 48.15848988, 154.93732625, 445.05562739, 1158.07779739, 16.63063166,
            3.01732209, 11.42247264, 37.37521902, 111.8752758, 306.5974569, 772.94026648
        ),
        difficulty = listOf(
            4.49094334, 4.26664289, 4.05746029, 3.86237659, 3.68044154, 3.51076891, 4.69833071,
            5.55956298, 5.26323756, 4.98688448, 4.72915759, 4.4888015, 4.26464541
        )
    )

    @Test
    fun review_startingWithAgain_matchesLongTermHistory() = assertHistory(
        grades = listOf(AGAIN, HARD, GOOD, EASY, AGAIN, HARD, GOOD, EASY),
        intervals = listOf(1, 2, 6, 41, 4, 7, 21, 133),
        stability = listOf(0.4197, 1.0344317, 5.5356759, 41.0033667, 4.46605519, 6.67743292, 20.88868155, 132.81849454),
        difficulty = listOf(7.1434, 7.03653841, 6.64066485, 5.92312772, 6.44779861, 6.45995078, 6.10293922, 5.36588547)
    )

    @Test
    fun review_startingWithHard_matchesLongTermHistory() = assertHistory(
        grades = listOf(HARD, GOOD, EASY, AGAIN, HARD, GOOD, EASY, AGAIN),
        intervals = listOf(2, 7, 54, 5, 8, 26, 171, 8),
        stability = listOf(
            1.1869,
            6.59167572,
            53.76078737,
            5.0853693,
            8.09786749,
            25.52991279,
            171.16195166,
            8.11072373
        ),
        difficulty = listOf(6.23225985, 5.89059466, 5.14583392, 5.884097, 5.99269555, 5.667177, 4.91430736, 5.71619151)
    )

    @Test
    fun review_startingWithGood_matchesLongTermHistory() = assertHistory(
        grades = listOf(GOOD, EASY, AGAIN, HARD, GOOD, EASY, AGAIN, HARD),
        intervals = listOf(3, 33, 4, 7, 26, 193, 9, 14),
        stability = listOf(
            3.0412,
            32.65484522,
            4.22256838,
            7.2325009,
            25.52681746,
            193.36618775,
            8.63899847,
            14.31323867
        ),
        difficulty = listOf(
            4.49094334,
            3.69538259,
            4.83221448,
            5.12078462,
            4.85403286,
            4.07165035,
            5.1050878,
            5.34697075
        )
    )

    @Test
    fun review_startingWithEasy_matchesLongTermHistory() = assertHistory(
        grades = listOf(EASY, AGAIN, HARD, GOOD, EASY, AGAIN, HARD, GOOD),
        intervals = listOf(15, 3, 6, 27, 240, 10, 17, 60),
        stability = listOf(
            15.2441,
            3.25621013,
            6.32684549,
            26.56339029,
            239.70462771,
            9.75621519,
            17.06035531,
            59.59547542
        ),
        difficulty = listOf(
            1.16304343,
            2.99573557,
            3.59851762,
            3.43436666,
            2.60045771,
            4.03816348,
            4.46259158,
            4.24020203
        )
    )

    @Test
    fun review_hardOnNewItem_waitsLongerThanAgain() {
        // Hard alone would round to one day, the same as Again.
        val hard = FsrsScheduler(FSRS5_LONG_TERM).review(memory = null, elapsedDays = 0, grade = HARD)

        assertThat(hard.intervalDays).isEqualTo(2)
    }

    @Test
    fun review_longStability_capsAtMaximumInterval() {
        val scheduler = FsrsScheduler(FsrsParameters(maximumIntervalDays = 30))

        val review = scheduler.review(MemoryState(stability = 100.0, difficulty = 5.0), elapsedDays = 100, grade = GOOD)

        assertThat(review.intervalDays).isEqualTo(30)
    }

    @Test
    fun review_againAfterLongGap_neverRaisesStability() {
        val memory = MemoryState(stability = 10.0, difficulty = 4.0)

        val review = scheduler.review(memory, elapsedDays = 1, grade = AGAIN)

        assertThat(review.memory.stability).isLessThan(memory.stability)
    }

    @Test
    fun review_sameDayGood_keepsStability() {
        val memory = MemoryState(stability = 10.0, difficulty = 4.0)

        val review = scheduler.review(memory, elapsedDays = 0, grade = GOOD)

        assertThat(review.memory.stability).isCloseTo(10.0, TOLERANCE)
    }

    /** Reviews each time the previous interval comes due, as the ts-fsrs long-term tests do. */
    private fun assertHistory(
        grades: List<Grade>,
        intervals: List<Int>,
        stability: List<Double>,
        difficulty: List<Double>
    ) {
        val scheduler = FsrsScheduler(FSRS5_LONG_TERM)
        var memory: MemoryState? = null
        var elapsed = 0
        grades.forEachIndexed { i, grade ->
            val review = scheduler.review(memory, elapsed, grade)
            assertThat(review.intervalDays, "interval $i").isEqualTo(intervals[i])
            assertThat(review.memory.stability, "stability $i").isCloseTo(stability[i], TOLERANCE)
            assertThat(review.memory.difficulty, "difficulty $i").isCloseTo(difficulty[i], TOLERANCE)
            memory = review.memory
            elapsed = review.intervalDays
        }
    }

    private companion object {
        const val TOLERANCE = 1e-4

        /** FSRS-5 weights from the long-term tests, with w19 = 0 and w20 = 0.5 as ts-fsrs migrates them. */
        val FSRS5_LONG_TERM = FsrsParameters(
            weights = listOf(
                0.4197, 1.1869, 3.0412, 15.2441, 7.1434, 0.6477, 1.0007, 0.0674, 1.6597, 0.1712, 1.1178,
                2.0225, 0.0904, 0.3025, 2.1214, 0.2498, 2.9466, 0.4891, 0.6468, 0.0, 0.5
            )
        )
    }
}
