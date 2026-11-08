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
package com.gyugle.gil.feature.stats.impl.domain

import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay

/** How many days ahead the review forecast looks, today included. */
const val FORECAST_DAYS = 7

/**
 * How many cards fall due on each of [days] days from [today]. Today's count includes every card
 * already overdue, since those wait for today's review too.
 */
fun reviewForecast(cards: List<CardState>, today: StudyDay, days: Int = FORECAST_DAYS): List<Int> {
    val counts = IntArray(days)
    cards.forEach { card ->
        val ahead = today.daysUntil(card.dueDay).coerceAtLeast(0)
        if (ahead < days) counts[ahead.toInt()]++
    }
    return counts.toList()
}
