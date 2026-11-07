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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import assertk.assertThat
import assertk.assertions.isGreaterThanOrEqualTo
import org.junit.jupiter.api.Named
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

/** WCAG 2.x contrast for every foreground and background pair the design system draws. */
class ColorContrastTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("textPairs")
    fun textPair_bothThemes_meetsBodyTextContrast(pair: Pair<(ColorScheme) -> Color, (ColorScheme) -> Color>) {
        assertBothThemes(pair, BODY_TEXT)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("graphicPairs")
    fun graphicPair_bothThemes_meetsNonTextContrast(pair: Pair<(ColorScheme) -> Color, (ColorScheme) -> Color>) {
        assertBothThemes(pair, NON_TEXT)
    }

    private fun assertBothThemes(pair: Pair<(ColorScheme) -> Color, (ColorScheme) -> Color>, minimum: Double) {
        val (foreground, background) = pair
        for (scheme in listOf(LightColorScheme, DarkColorScheme)) {
            assertThat(contrast(foreground(scheme), background(scheme))).isGreaterThanOrEqualTo(minimum)
        }
    }

    companion object {
        private const val BODY_TEXT = 4.5
        private const val NON_TEXT = 3.0

        private val surfaces: List<Pair<String, (ColorScheme) -> Color>> = listOf(
            "surface" to { it.surface },
            "surfaceContainerLow" to { it.surfaceContainerLow },
            "surfaceContainer" to { it.surfaceContainer },
            "surfaceContainerHigh" to { it.surfaceContainerHigh }
        )

        private val textOnSurfaces: List<Pair<String, (ColorScheme) -> Color>> = listOf(
            "onSurface" to { it.onSurface },
            "onSurfaceVariant" to { it.onSurfaceVariant },
            "primary" to { it.primary },
            "secondary" to { it.secondary },
            "tertiary" to { it.tertiary },
            "error" to { it.error }
        )

        private val textOnFills: List<Triple<String, (ColorScheme) -> Color, (ColorScheme) -> Color>> = listOf(
            Triple("onPrimary on primary", { it.onPrimary }, { it.primary }),
            Triple("onPrimaryContainer on primaryContainer", { it.onPrimaryContainer }, { it.primaryContainer }),
            Triple("primary on primaryContainer", { it.primary }, { it.primaryContainer }),
            Triple("onSecondary on secondary", { it.onSecondary }, { it.secondary }),
            Triple("onSecondaryContainer on secondaryContainer", {
                it.onSecondaryContainer
            }, { it.secondaryContainer }),
            Triple("onTertiary on tertiary", { it.onTertiary }, { it.tertiary }),
            Triple("onTertiaryContainer on tertiaryContainer", { it.onTertiaryContainer }, { it.tertiaryContainer }),
            Triple("onError on error", { it.onError }, { it.error }),
            Triple("onErrorContainer on errorContainer", { it.onErrorContainer }, { it.errorContainer }),
            Triple("inverseOnSurface on inverseSurface", { it.inverseOnSurface }, { it.inverseSurface }),
            Triple("inversePrimary on inverseSurface", { it.inversePrimary }, { it.inverseSurface })
        )

        @JvmStatic
        fun textPairs(): List<Arguments> = textOnSurfaces.flatMap { (fgName, fg) ->
            surfaces.map { (bgName, bg) -> named("$fgName on $bgName", fg, bg) }
        } + textOnFills.map { (name, fg, bg) -> named(name, fg, bg) }

        @JvmStatic
        fun graphicPairs(): List<Arguments> = surfaces.flatMap { (bgName, bg) ->
            listOf(
                named("outline on $bgName", { it.outline }, bg),
                named("tertiary on $bgName", { it.tertiary }, bg)
            )
        } + named("primary on primaryContainer track", { it.primary }, { it.primaryContainer })

        private fun named(name: String, fg: (ColorScheme) -> Color, bg: (ColorScheme) -> Color): Arguments =
            Arguments.of(Named.of(name, fg to bg))

        private fun contrast(a: Color, b: Color): Double {
            val lighter = maxOf(a.luminance(), b.luminance())
            val darker = minOf(a.luminance(), b.luminance())
            return (lighter + 0.05) / (darker + 0.05)
        }
    }
}
