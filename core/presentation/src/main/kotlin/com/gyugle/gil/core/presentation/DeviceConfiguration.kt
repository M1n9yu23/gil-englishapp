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
package com.gyugle.gil.core.presentation

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.HEIGHT_DP_MEDIUM_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND

@Composable
fun currentDeviceConfiguration(): DeviceConfiguration =
    DeviceConfiguration.fromWindowSizeClass(currentWindowAdaptiveInfoV2().windowSizeClass)

/** The window shapes a layout may change its structure for. Sizing alone does not need this. */
enum class DeviceConfiguration {
    MOBILE_PORTRAIT,
    MOBILE_LANDSCAPE,
    TABLET_PORTRAIT,
    TABLET_LANDSCAPE,
    DESKTOP
    ;

    val isMobile: Boolean get() = this == MOBILE_PORTRAIT || this == MOBILE_LANDSCAPE

    val isWideScreen: Boolean get() = this == TABLET_LANDSCAPE || this == DESKTOP

    companion object {
        /**
         * Uses the breakpoint checks, not `minWidthDp`: the window info buckets sizes, so
         * `minWidthDp` is a bucket's lower bound rather than the real width.
         */
        fun fromWindowSizeClass(windowSizeClass: WindowSizeClass): DeviceConfiguration {
            val mediumWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND)
            val expandedWidth = windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
            val mediumHeight = windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_MEDIUM_LOWER_BOUND)
            val expandedHeight = windowSizeClass.isHeightAtLeastBreakpoint(HEIGHT_DP_EXPANDED_LOWER_BOUND)
            // Height first: a short window cannot hold a vertical stack, whatever its width.
            return when {
                !mediumHeight -> if (mediumWidth) MOBILE_LANDSCAPE else MOBILE_PORTRAIT
                !mediumWidth -> MOBILE_PORTRAIT
                !expandedWidth -> TABLET_PORTRAIT
                !expandedHeight -> TABLET_LANDSCAPE
                else -> DESKTOP
            }
        }
    }
}
