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
package com.gyugle.gil.feature.settings.impl.data.repository

import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.api.ThemeMode

/*
 * How each choice is written, both in settings_prefs and in a backup file. These strings are kept
 * on phones and in files people save, so renaming a constant must not change them.
 */

internal val ThemeMode.key: String
    get() = when (this) {
        ThemeMode.SYSTEM -> "system"
        ThemeMode.LIGHT -> "light"
        ThemeMode.DARK -> "dark"
    }

internal fun themeOf(key: String?): ThemeMode? = ThemeMode.entries.firstOrNull { it.key == key }

internal val Accent.key: String
    get() = when (this) {
        Accent.AMERICAN -> "american"
        Accent.BRITISH -> "british"
    }

internal fun accentOf(key: String?): Accent? = Accent.entries.firstOrNull { it.key == key }

internal val SpeechSpeed.key: String
    get() = when (this) {
        SpeechSpeed.NORMAL -> "normal"
        SpeechSpeed.SLOW -> "slow"
    }

internal fun speedOf(key: String?): SpeechSpeed? = SpeechSpeed.entries.firstOrNull { it.key == key }
