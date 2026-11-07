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

import com.gyugle.gil.feature.progress.impl.domain.model.MemoryState
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * The FSRS-6 memory model (Free Spaced Repetition Scheduler, open-spaced-repetition project),
 * scheduled in whole days.
 *
 * Formulas follow https://github.com/open-spaced-repetition/awesome-fsrs/wiki/The-Algorithm. The
 * app reviews at most once per due day, so this is the "long-term" variant: no same-day learning
 * steps, and a review on the day of the last one is treated like any other with no time passed.
 * Intervals are not fuzzed, so items learned together come due together.
 */
class FsrsScheduler(private val parameters: FsrsParameters = FsrsParameters()) {
    enum class Grade(internal val value: Int) {
        AGAIN(1),
        HARD(2),
        GOOD(3),
        EASY(4)
    }

    /** The new memory and the days until the next review. */
    data class Review(val memory: MemoryState, val intervalDays: Int)

    private val w = parameters.weights
    private val decay = w[FsrsParameters.DECAY]

    /** Chosen so that recall is exactly 90% when [elapsedDays] equals the stability. */
    private val factor = 0.9.pow(-1 / decay) - 1

    /** The chance of recalling an item with [stability] after [elapsedDays]. */
    fun retrievability(elapsedDays: Double, stability: Double): Double =
        (1 + factor * elapsedDays / stability).pow(-decay)

    /**
     * Applies [grade] to [memory], or starts a memory when it is null, after [elapsedDays] since
     * the last review.
     */
    fun nextMemory(memory: MemoryState?, elapsedDays: Int, grade: Grade): MemoryState {
        require(elapsedDays >= 0) { "Elapsed days must not be negative" }
        if (memory == null) {
            return MemoryState(
                stability = initialStability(grade),
                difficulty = initialDifficulty(grade).coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
            )
        }
        val r = retrievability(elapsedDays.toDouble(), memory.stability)
        val stability = when (grade) {
            // Forgetting never leaves an item more stable than it was.
            Grade.AGAIN -> min(max(memory.stability, MIN_STABILITY), forgetStability(memory, r))

            else -> recallStability(memory, r, grade)
        }
        return MemoryState(stability, nextDifficulty(memory.difficulty, grade))
    }

    /**
     * Rates a review and picks the next interval.
     *
     * A better grade never gets a shorter interval than a worse one: below the maximum interval,
     * each grade waits at least a day longer than the one below it.
     */
    fun review(memory: MemoryState?, elapsedDays: Int, grade: Grade): Review {
        var interval = 0
        lateinit var chosen: MemoryState
        for (g in Grade.entries) {
            val next = nextMemory(memory, elapsedDays, g)
            val candidate = nextInterval(next.stability)
            interval =
                min(if (g == Grade.AGAIN) candidate else max(candidate, interval + 1), parameters.maximumIntervalDays)
            if (g == grade) {
                chosen = next
                break
            }
        }
        return Review(chosen, interval)
    }

    /** The whole days until recall falls to the desired retention, at least one. */
    fun nextInterval(stability: Double): Int {
        val days = stability / factor * (parameters.desiredRetention.pow(-1 / decay) - 1)
        return max(days.roundToLong(), 1).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun initialStability(grade: Grade): Double = w[grade.value - 1].coerceIn(MIN_STABILITY, MAX_STABILITY)

    private fun initialDifficulty(grade: Grade): Double = w[4] - exp((grade.value - 1) * w[5]) + 1

    private fun nextDifficulty(difficulty: Double, grade: Grade): Double {
        val delta = -w[6] * (grade.value - 3)
        // Linear damping: the closer to 10, the smaller each step up.
        val damped = difficulty + delta * (10 - difficulty) / 9
        // Mean reversion toward the difficulty of a first "easy", so it cannot drift forever.
        val reverted = w[7] * initialDifficulty(Grade.EASY) + (1 - w[7]) * damped
        return reverted.coerceIn(MIN_DIFFICULTY, MAX_DIFFICULTY)
    }

    internal fun recallStability(memory: MemoryState, r: Double, grade: Grade): Double {
        val (s, d) = memory
        val hardPenalty = if (grade == Grade.HARD) w[15] else 1.0
        val easyBonus = if (grade == Grade.EASY) w[16] else 1.0
        val growth = exp(w[8]) * (11 - d) * s.pow(-w[9]) * (exp((1 - r) * w[10]) - 1) * hardPenalty * easyBonus
        return (s * (1 + growth)).coerceIn(MIN_STABILITY, MAX_STABILITY)
    }

    internal fun forgetStability(memory: MemoryState, r: Double): Double {
        val (s, d) = memory
        val stability = w[11] * d.pow(-w[12]) * ((s + 1).pow(w[13]) - 1) * exp((1 - r) * w[14])
        return stability.coerceIn(MIN_STABILITY, MAX_STABILITY)
    }

    private companion object {
        const val MIN_STABILITY = 0.001
        const val MAX_STABILITY = 36_500.0
        const val MIN_DIFFICULTY = 1.0
        const val MAX_DIFFICULTY = 10.0
    }
}
