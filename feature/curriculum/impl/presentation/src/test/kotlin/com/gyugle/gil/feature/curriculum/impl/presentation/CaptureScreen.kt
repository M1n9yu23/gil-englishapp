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
package com.gyugle.gil.feature.curriculum.impl.presentation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.gyugle.gil.core.designsystem.theme.GilTheme

/** Past the state crossfade, and the same instant in every image. */
private const val SETTLED_MILLIS = 300L

/** Renders a screen on the paper surface with the clock held and saves it as a PNG named after the test. */
fun ComposeContentTestRule.captureScreen(content: @Composable () -> Unit) {
    mainClock.autoAdvance = false
    setContent {
        GilTheme {
            Surface(content = content)
        }
    }
    mainClock.advanceTimeBy(SETTLED_MILLIS)
    onRoot().captureRoboImage()
}
