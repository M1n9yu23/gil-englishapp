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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import com.gyugle.gil.feature.curriculum.api.LessonStep
import kotlin.random.Random

/**
 * Choices and tiles appear in an order fixed by [seed], so a rotation or a return to the lesson
 * shows the same order, while the content's own order (right answer first) never shows.
 */
internal class StepLayout(step: LessonStep, private val seed: Int) {
    /** Every tile, answer and extra, in display order. */
    val bank: List<String> = (step as? LessonStep.Arrange)?.let {
        (it.tiles + it.extra).shuffled(Random(seed))
    }.orEmpty()

    private val keepsOrder = step is LessonStep.MinimalPair

    /**
     * Display position to the content's index, for [size] options. Each passage question passes
     * its own [question], so the right answer does not sit in the same place every time. A sound
     * pair keeps its own order.
     */
    fun order(size: Int, question: Int = 0): List<Int> =
        if (keepsOrder) (0 until size).toList() else (0 until size).shuffled(Random(seed * 31 + question))
}
