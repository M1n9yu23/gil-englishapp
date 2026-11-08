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
package com.gyugle.gil.feature.settings.api

/**
 * The choices on the settings screen that other features follow. The daily goal and the reminder
 * times have their own owners and are not here.
 *
 * @property showHangulAid whether a word page shows its sound in Hangul. The content gives that aid
 *   only for the words of the first stages, so turning it on shows nothing for later words.
 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: Accent = Accent.AMERICAN,
    val speed: SpeechSpeed = SpeechSpeed.NORMAL,
    val showHangulAid: Boolean = true
)

enum class ThemeMode {
    /** Light or dark as the phone is set. */
    SYSTEM,
    LIGHT,
    DARK
}

/** The English voice that reads lessons and reviews. */
enum class Accent {
    AMERICAN,
    BRITISH
}

/** How fast lessons and reviews play English the first time; the slow button stays slow. */
enum class SpeechSpeed {
    NORMAL,
    SLOW
}
