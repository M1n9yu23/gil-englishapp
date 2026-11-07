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

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Paper and ink. Every slot is set so no Material baseline colour can leak in.
// outlineVariant (about 1.4:1 on paper) is for decorative dividers only; borders of controls use outline.

internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF2F4A86),
    onPrimary = Color(0xFFFCFAF5),
    primaryContainer = Color(0xFFDCE3F4),
    onPrimaryContainer = Color(0xFF1B2C55),
    inversePrimary = Color(0xFFA9BCF0),
    secondary = Color(0xFF5B6170),
    onSecondary = Color(0xFFFCFAF5),
    secondaryContainer = Color(0xFFE3E4E8),
    onSecondaryContainer = Color(0xFF2A2F3A),
    tertiary = Color(0xFFA93A29),
    onTertiary = Color(0xFFFCFAF5),
    tertiaryContainer = Color(0xFFF6DCD5),
    onTertiaryContainer = Color(0xFF5E1A10),
    background = Color(0xFFF4F3EF),
    onBackground = Color(0xFF1F2A44),
    surface = Color(0xFFF4F3EF),
    onSurface = Color(0xFF1F2A44),
    surfaceVariant = Color(0xFFE1E0DC),
    onSurfaceVariant = Color(0xFF4E566B),
    surfaceTint = Color(0xFF2F4A86),
    inverseSurface = Color(0xFF2E3546),
    inverseOnSurface = Color(0xFFECE6D9),
    error = Color(0xFF83550F),
    onError = Color(0xFFFCFAF5),
    errorContainer = Color(0xFFF3E2C4),
    onErrorContainer = Color(0xFF3D2A05),
    outline = Color(0xFF7D776A),
    outlineVariant = Color(0xFFD4D2CC),
    scrim = Color(0xFF16191F),
    surfaceBright = Color(0xFFF4F3EF),
    surfaceContainer = Color(0xFFE8E7E3),
    surfaceContainerHigh = Color(0xFFE1E0DC),
    surfaceContainerHighest = Color(0xFFDAD9D5),
    surfaceContainerLow = Color(0xFFEEEDE9),
    surfaceContainerLowest = Color(0xFFFAF9F6),
    surfaceDim = Color(0xFFDAD9D5),
    primaryFixed = Color(0xFFDCE3F4),
    primaryFixedDim = Color(0xFFA9BCF0),
    onPrimaryFixed = Color(0xFF13244D),
    onPrimaryFixedVariant = Color(0xFF2A3F72),
    secondaryFixed = Color(0xFFE3E4E8),
    secondaryFixedDim = Color(0xFFC6C8CF),
    onSecondaryFixed = Color(0xFF1E222B),
    onSecondaryFixedVariant = Color(0xFF444956),
    tertiaryFixed = Color(0xFFF6DCD5),
    tertiaryFixedDim = Color(0xFFF0A48F),
    onTertiaryFixed = Color(0xFF3B0F08),
    onTertiaryFixedVariant = Color(0xFF5E1A10)
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFA9BCF0),
    onPrimary = Color(0xFF13244D),
    primaryContainer = Color(0xFF2A3F72),
    onPrimaryContainer = Color(0xFFDCE3F4),
    inversePrimary = Color(0xFF2F4A86),
    secondary = Color(0xFFC3C1BA),
    onSecondary = Color(0xFF2B2D33),
    secondaryContainer = Color(0xFF3A3F4A),
    onSecondaryContainer = Color(0xFFE2E0D9),
    tertiary = Color(0xFFF0A48F),
    onTertiary = Color(0xFF4A140B),
    tertiaryContainer = Color(0xFF5E1A10),
    onTertiaryContainer = Color(0xFFF6DCD5),
    background = Color(0xFF16191F),
    onBackground = Color(0xFFECE6D9),
    surface = Color(0xFF16191F),
    onSurface = Color(0xFFECE6D9),
    surfaceVariant = Color(0xFF262B35),
    onSurfaceVariant = Color(0xFFB5B0A4),
    surfaceTint = Color(0xFFA9BCF0),
    inverseSurface = Color(0xFFECE6D9),
    inverseOnSurface = Color(0xFF1F2A44),
    error = Color(0xFFE0B56A),
    onError = Color(0xFF3D2A05),
    errorContainer = Color(0xFF5A3D0C),
    onErrorContainer = Color(0xFFF3E2C4),
    outline = Color(0xFF8E897E),
    outlineVariant = Color(0xFF3A3F4A),
    scrim = Color(0xFF0E1014),
    surfaceBright = Color(0xFF2E333E),
    surfaceContainer = Color(0xFF20242D),
    surfaceContainerHigh = Color(0xFF262B35),
    surfaceContainerHighest = Color(0xFF2E333E),
    surfaceContainerLow = Color(0xFF1C2028),
    surfaceContainerLowest = Color(0xFF111419),
    surfaceDim = Color(0xFF16191F),
    primaryFixed = Color(0xFFDCE3F4),
    primaryFixedDim = Color(0xFFA9BCF0),
    onPrimaryFixed = Color(0xFF13244D),
    onPrimaryFixedVariant = Color(0xFF2A3F72),
    secondaryFixed = Color(0xFFE3E4E8),
    secondaryFixedDim = Color(0xFFC6C8CF),
    onSecondaryFixed = Color(0xFF1E222B),
    onSecondaryFixedVariant = Color(0xFF444956),
    tertiaryFixed = Color(0xFFF6DCD5),
    tertiaryFixedDim = Color(0xFFF0A48F),
    onTertiaryFixed = Color(0xFF3B0F08),
    onTertiaryFixedVariant = Color(0xFF5E1A10)
)
