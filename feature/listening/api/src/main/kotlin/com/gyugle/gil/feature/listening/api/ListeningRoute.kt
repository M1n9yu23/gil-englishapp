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
package com.gyugle.gil.feature.listening.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ListeningRoute : NavKey

/**
 * Listening mode: pick what to hear, then listen with the screen off. While something is playing,
 * it opens on the player instead.
 */
@Serializable
@SerialName("listening.player")
data object ListeningPlayerRoute : ListeningRoute

/**
 * The data URI that opens listening mode from outside the app, such as from its media
 * notification. Put it on an explicit intent to the app's launch activity.
 */
const val LISTENING_DEEP_LINK = "gil://listening"
