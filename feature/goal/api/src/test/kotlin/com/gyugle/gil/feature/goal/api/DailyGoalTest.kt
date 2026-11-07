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
package com.gyugle.gil.feature.goal.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

class DailyGoalTest {

    @Test
    fun isMetBy_exactlyTheGoal_isMet() {
        assertThat(DailyGoal.TWENTY.isMetBy(20.minutes)).isTrue()
    }

    @Test
    fun isMetBy_oneSecondShort_isNotMet() {
        assertThat(DailyGoal.TWENTY.isMetBy(20.minutes - 1.seconds)).isFalse()
    }

    @Test
    fun ofMinutes_eachGoal_roundTrips() {
        DailyGoal.entries.forEach { goal -> assertThat(DailyGoal.ofMinutes(goal.minutes)).isEqualTo(goal) }
    }

    @Test
    fun ofMinutes_unknownLength_isNull() {
        assertThat(DailyGoal.ofMinutes(15)).isNull()
    }
}
