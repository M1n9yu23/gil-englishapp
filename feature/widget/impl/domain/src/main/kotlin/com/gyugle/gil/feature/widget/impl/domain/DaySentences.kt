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
package com.gyugle.gil.feature.widget.impl.domain

import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay
import kotlin.random.Random

/**
 * Items whose examples can be [today]'s sentence: words and patterns met before today. Items first
 * met today are left out, so finishing a lesson does not change the sentence halfway through the
 * day, and the sentence is always something the learner has slept on. Sounds have no examples.
 */
fun sentenceItems(today: StudyDay, cards: List<CardState>): List<StudyItemId> = cards
    .filterNot { it.itemId is SoundId || (it.lastReviewDay == today && it.reviewCount == 1) }
    .map { it.itemId }

/**
 * [items] in [day]'s order: the same all day, whatever order they came in, and shuffled again the
 * next day.
 */
fun dayOrder(day: StudyDay, items: List<StudyItemId>): List<StudyItemId> =
    items.sortedBy { it.value }.shuffled(Random(day.epochDay))

/** Which of an item's [count] examples [day] shows: the next one each day. */
fun exampleIndex(day: StudyDay, count: Int): Int = day.epochDay.mod(count)
