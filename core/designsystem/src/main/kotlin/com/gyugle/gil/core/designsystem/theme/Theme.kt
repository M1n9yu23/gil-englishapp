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
package com.gyugle.gil.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable

/**
 * The only provider of Gil's tokens. Colours, type and shapes live in the Material slots; spacing
 * and motion, which Material has no slot for, are read through [GilTheme.spacing] and
 * [GilTheme.motion]. There is no dynamic colour.
 */
@Composable
fun GilTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalSpacing provides Spacing(),
        LocalMotion provides Motion()
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = GilTypography,
            shapes = GilShapes,
            content = content
        )
    }
}

object GilTheme {
    val spacing: Spacing
        @Composable @ReadOnlyComposable
        get() = LocalSpacing.current

    val motion: Motion
        @Composable @ReadOnlyComposable
        get() = LocalMotion.current
}
