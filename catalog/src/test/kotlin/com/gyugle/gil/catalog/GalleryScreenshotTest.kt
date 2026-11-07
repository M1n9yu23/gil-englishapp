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
package com.gyugle.gil.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Same frozen instant as the core:designsystem captures. */
private const val FROZEN_FRAME_MILLIS = 300L

/**
 * The token sections of the gallery. A changed colour or type token shows up here first; the
 * components have their own baselines in core:designsystem.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h1400dp-xhdpi")
class GalleryScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(content: @Composable () -> Unit) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { GilTheme { Surface { Column { content() } } } }
        composeRule.mainClock.advanceTimeBy(FROZEN_FRAME_MILLIS)
        composeRule.onRoot().captureRoboImage()
    }

    private fun colors() = capture { GallerySection(R.string.catalog_section_colors) { ColorTokens() } }

    private fun type() = capture { GallerySection(R.string.catalog_section_type) { TypeTokens() } }

    @Test fun colors_light_matchesPalette() = colors()

    @Test
    @Config(qualifiers = "+night")
    fun colors_night_matchesPalette() = colors()

    @Test fun type_light_matchesScale() = type()

    @Test
    @Config(qualifiers = "+night")
    fun type_night_matchesScale() = type()

    @Test
    @Config(fontScale = 2f)
    fun type_fontScale2_wrapsSamples() = type()
}
