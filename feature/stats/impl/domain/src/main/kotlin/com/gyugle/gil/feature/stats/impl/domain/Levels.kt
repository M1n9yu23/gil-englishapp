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
import com.gyugle.gil.feature.stats.impl.domain.model.ItemLevel
import com.gyugle.gil.feature.stats.impl.domain.model.LevelCounts

/** A card that holds this many days or more is [ItemLevel.FAMILIAR]. */
const val FAMILIAR_STABILITY_DAYS = 7.0

/** A card that holds this many days or more is [ItemLevel.LASTING]. */
const val LASTING_STABILITY_DAYS = 21.0

/** The level of the item, from how many days the card holds before recall falls to 90%. */
fun CardState.level(): ItemLevel = when {
    stabilityDays < FAMILIAR_STABILITY_DAYS -> ItemLevel.LEARNING
    stabilityDays < LASTING_STABILITY_DAYS -> ItemLevel.FAMILIAR
    else -> ItemLevel.LASTING
}

fun countLevels(cards: List<CardState>): LevelCounts {
    val levels = cards.groupingBy { it.level() }.eachCount()
    return LevelCounts(
        learning = levels[ItemLevel.LEARNING] ?: 0,
        familiar = levels[ItemLevel.FAMILIAR] ?: 0,
        lasting = levels[ItemLevel.LASTING] ?: 0
    )
}
