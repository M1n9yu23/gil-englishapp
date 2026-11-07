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

import com.gyugle.gil.feature.curriculum.api.SearchHit
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.dictionary.impl.domain.model.RankedHit

/**
 * Puts the hits the learner has met first: a hit is learned when any of its meanings has a card in
 * [learned]. Within each group the content's order stays, which already puts exact headwords first.
 */
fun List<SearchHit>.learnedFirst(learned: Set<StudyItemId>): List<RankedHit> = map { hit ->
    RankedHit(hit = hit, learned = hit.meanings.any { it.id in learned })
}.sortedBy { !it.learned }
