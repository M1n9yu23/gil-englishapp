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

/**
 * One entry of a playlist as it is heard: what is said and the pauses between, in order. It is one
 * step for "next" and "previous" on the lock screen and the headphones. [title] is English and
 * [subtitle] its Korean, shown on the player and the media notification.
 */
data class Track(val id: String, val title: String, val subtitle: String?, val parts: List<TrackPart>)

sealed interface TrackPart {
    data class Speech(val text: String, val language: SpokenLanguage) : TrackPart

    data class Pause(val duration: Duration) : TrackPart
}

/** The English voice follows the accent in the settings. */
enum class SpokenLanguage {
    ENGLISH,
    KOREAN
}
