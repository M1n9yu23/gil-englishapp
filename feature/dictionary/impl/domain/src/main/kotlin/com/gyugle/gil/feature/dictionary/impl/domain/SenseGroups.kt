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
package com.gyugle.gil.feature.dictionary.impl.domain

import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.dictionary.impl.domain.model.SenseGroups

/**
 * Splits a word's senses into what to know now and what comes later, each in the content's order.
 *
 * The senses the learner has met are the ones to know now. A word met in no sense, such as one
 * found by search, shows the senses of its earliest stage instead, the ones the course teaches
 * first. [focus], the sense a list opened the word for, is always among the ones to know now, but
 * only a met sense makes the word learned.
 */
fun groupSenses(senses: List<WordSense>, learned: Set<StudyItemId>, focus: SenseId? = null): SenseGroups {
    val learnedAny = senses.any { it.id in learned }
    val firstStage = senses.minOfOrNull { it.stage.position }
    val (now, later) = senses.partition { sense ->
        sense.id == focus || if (learnedAny) sense.id in learned else sense.stage.position == firstStage
    }
    return SenseGroups(now = now, later = later, learnedAny = learnedAny)
}
