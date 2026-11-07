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

/**
 * Settings for [FsrsScheduler].
 *
 * @property weights the 21 FSRS-6 weights, w0 to w20. w20 is the decay of the forgetting curve.
 * @property desiredRetention the chance of recall at which an item comes due.
 * @property maximumIntervalDays the longest gap between two reviews.
 */
data class FsrsParameters(
    val weights: List<Double> = DEFAULT_WEIGHTS,
    val desiredRetention: Double = 0.9,
    val maximumIntervalDays: Int = 36_500
) {
    init {
        require(weights.size == WEIGHT_COUNT) { "FSRS-6 needs $WEIGHT_COUNT weights, got ${weights.size}" }
        require(weights.all { it.isFinite() }) { "Weights must be finite" }
        require(weights[DECAY] > 0) { "The decay weight w20 must be positive" }
        require(desiredRetention > 0 && desiredRetention < 1) { "Desired retention must be between 0 and 1" }
        require(maximumIntervalDays >= 1) { "The maximum interval must be at least one day" }
    }

    companion object {
        const val WEIGHT_COUNT = 21
        internal const val DECAY = 20

        /** The FSRS-6 defaults, trained on many learners' review logs. */
        val DEFAULT_WEIGHTS: List<Double> = listOf(
            0.212, 1.2931, 2.3065, 8.2956, 6.4133, 0.8334, 3.0194, 0.001, 1.8722, 0.1666, 0.796,
            1.4835, 0.0614, 0.2629, 1.6483, 0.6014, 1.8729, 0.5425, 0.0912, 0.0658, 0.1542
        )
    }
}
