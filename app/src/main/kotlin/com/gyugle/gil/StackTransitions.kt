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
package com.gyugle.gil

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.theme.GilTheme

private object StackTransitionTokens {
    /** A short shift is enough to show direction; a full-width slide would read as travel. */
    val SlideDistance = 30.dp
}

/** The transitions of a host: a fade between tabs, a short slide within one stack. */
internal class StackTransitions(val fade: ContentTransform, val push: ContentTransform, val pop: ContentTransform)

@Composable
internal fun rememberStackTransitions(): StackTransitions {
    val motion = GilTheme.motion
    val distance = with(LocalDensity.current) { StackTransitionTokens.SlideDistance.roundToPx() }
    return remember(motion, distance) {
        val enter = tween<Float>(motion.enter, easing = motion.enterEasing)
        val exit = tween<Float>(motion.exit, easing = motion.exitEasing)
        val enterOffset = tween<IntOffset>(motion.enter, easing = motion.enterEasing)
        val exitOffset = tween<IntOffset>(motion.exit, easing = motion.exitEasing)
        StackTransitions(
            fade = fadeIn(tween(motion.fade)) togetherWith fadeOut(tween(motion.fade)),
            push = (slideInHorizontally(enterOffset) { distance } + fadeIn(enter)) togetherWith
                (slideOutHorizontally(exitOffset) { -distance } + fadeOut(exit)),
            pop = (slideInHorizontally(enterOffset) { -distance } + fadeIn(enter)) togetherWith
                (slideOutHorizontally(exitOffset) { distance } + fadeOut(exit))
        )
    }
}
