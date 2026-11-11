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

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class Spacing(
    /** Start and end edge of every screen. */
    val screenMargin: Dp = 20.dp,
    val sectionGap: Dp = 32.dp,
    /** Between rows, and between blocks inside one group. */
    val itemGap: Dp = 12.dp,
    /** Between an icon and its label, or a title and the line under it. */
    val inlineGap: Dp = 8.dp,
    /**
     * The widest a screen gets. A wider window keeps the screen centred, so lines stay short
     * enough to read and a row's end stays near its start. A phone held upright never reaches it.
     */
    val contentMaxWidth: Dp = 600.dp
)

/** Durations in milliseconds. Compose scales them by the system animation setting. */
@Immutable
data class Motion(
    val pressIn: Int = 90,
    val pressOut: Int = 160,
    /** A value or state changing in place. */
    val fade: Int = 150,
    /** A screen coming in, with [enterEasing]. */
    val enter: Int = 250,
    /** A screen going out, with [exitEasing]. */
    val exit: Int = 200,
    /** An answer shown after checking: slower than [fade], like ink soaking into paper. */
    val reveal: Int = 300,
    /** Material's emphasized decelerate: a quick start that settles gently. */
    val enterEasing: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
    /** Material's emphasized accelerate: a gentle start that leaves quickly. */
    val exitEasing: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
)

// Internal so that GilTheme stays the only place that provides them.
internal val LocalSpacing = staticCompositionLocalOf { Spacing() }
internal val LocalMotion = staticCompositionLocalOf { Motion() }
