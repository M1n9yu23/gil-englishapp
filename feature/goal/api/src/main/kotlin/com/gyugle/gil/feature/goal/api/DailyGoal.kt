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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** How long the learner means to study each day. Onboarding and settings offer exactly these. */
enum class DailyGoal(val minutes: Int) {
    TEN(10),
    TWENTY(20),
    THIRTY(30),
    SIXTY(60);

    val duration: Duration get() = minutes.minutes

    /** Whether [studied] in one study day reaches this goal. */
    fun isMetBy(studied: Duration): Boolean = studied >= duration

    companion object {
        /** The goal until the learner picks one, which is also what skipping onboarding leaves. */
        val DEFAULT = TWENTY

        /** The goal of [minutes], or null when no goal has that length. */
        fun ofMinutes(minutes: Int): DailyGoal? = entries.firstOrNull { it.minutes == minutes }
    }
}
