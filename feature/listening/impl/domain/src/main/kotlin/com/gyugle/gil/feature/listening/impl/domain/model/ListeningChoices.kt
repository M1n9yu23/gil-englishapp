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
package com.gyugle.gil.feature.listening.impl.domain.model

import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.UnitId

/**
 * What there is to listen to, as the number of tracks each source would play. [units] lists only
 * the units with items in finished lessons, in course order.
 */
data class ListeningChoices(val dueToday: Int, val recent: Int, val dialogues: Int, val units: List<UnitChoice>) {
    /** The learner has finished no lesson with anything to hear. */
    val isEmpty: Boolean get() = recent == 0 && dialogues == 0
}

data class UnitChoice(val id: UnitId, val title: LocalizedText, val itemCount: Int)
