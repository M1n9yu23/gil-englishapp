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

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How listening mode plays: how fast the English is and how long the learner has to recall. */
data class ListeningPace(val speed: ListeningSpeed = ListeningSpeed.NORMAL, val pause: PauseLength = PauseLength.MEDIUM)

/** Only English slows down; Korean always plays at the normal speed. */
enum class ListeningSpeed {
    NORMAL,
    SLOW
}

/** The silence after an English word or line, to recall its meaning before the Korean comes. */
enum class PauseLength(val duration: Duration) {
    SHORT(2.seconds),
    MEDIUM(3.seconds),
    LONG(5.seconds)
}
