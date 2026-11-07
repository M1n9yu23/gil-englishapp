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
package com.gyugle.gil.feature.progress.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import org.junit.jupiter.api.Test

class StudyDayTest {
    @Test
    fun plusAndMinus_moveByWholeDays() {
        assertThat(StudyDay(100) + 3).isEqualTo(StudyDay(103))
        assertThat(StudyDay(100) - 3).isEqualTo(StudyDay(97))
    }

    @Test
    fun daysUntil_earlierDay_isNegative() {
        assertThat(StudyDay(100).daysUntil(StudyDay(104))).isEqualTo(4)
        assertThat(StudyDay(100).daysUntil(StudyDay(98))).isEqualTo(-2)
    }

    @Test
    fun compareTo_ordersByDate() {
        assertThat(StudyDay(101)).isGreaterThan(StudyDay(100))
    }
}
