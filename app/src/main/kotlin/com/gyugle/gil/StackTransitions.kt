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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.navigation.isTaskScreen

private object StackTransitionTokens {
    /** A short shift is enough to show direction; a full-width slide would read as travel. */
    val SlideDistance = 30.dp

    /**
     * How long the screen going out takes to fade. The one coming in starts to show only then, so
     * the two never read through each other halfway.
     */
    const val HAND_OFF = 90
}

/**
 * The transitions of a host: a fade between tabs, a short slide within one stack, and a short rise
 * for a task screen such as a lesson, which sinks back when it closes.
 */
internal class StackTransitions(
    /** A screen fading in once the one before it has gone, for anything that comes with a screen. */
    val arrive: EnterTransition,
    /** A screen fading out before the next one shows. */
    val leave: ExitTransition,
    val fade: ContentTransform,
    val push: ContentTransform,
    val pop: ContentTransform,
    val rise: ContentTransform,
    val sink: ContentTransform
)

@Composable
internal fun rememberStackTransitions(): StackTransitions {
    val motion = GilTheme.motion
    val distance = with(LocalDensity.current) { StackTransitionTokens.SlideDistance.roundToPx() }
    return remember(motion, distance) {
        val handOff = StackTransitionTokens.HAND_OFF
        val leave = fadeOut(tween(handOff, easing = motion.exitEasing))
        val arrive = fadeIn(tween(motion.enter - handOff, delayMillis = handOff, easing = motion.enterEasing))
        val enterOffset = tween<IntOffset>(motion.enter, easing = motion.enterEasing)
        val exitOffset = tween<IntOffset>(motion.exit, easing = motion.exitEasing)
        StackTransitions(
            arrive = arrive,
            leave = leave,
            fade = fadeIn(tween(motion.fade)) togetherWith fadeOut(tween(motion.fade)),
            push = (slideInHorizontally(enterOffset) { distance } + arrive) togetherWith
                (slideOutHorizontally(exitOffset) { -distance } + leave),
            pop = (slideInHorizontally(enterOffset) { -distance } + arrive) togetherWith
                (slideOutHorizontally(exitOffset) { distance } + leave),
            // The screen underneath stays where it is; only the task moves.
            rise = (slideInVertically(enterOffset) { distance } + arrive) togetherWith leave,
            sink = arrive togetherWith (slideOutVertically(exitOffset) { distance } + leave)
        )
    }
}

/**
 * The transition from [initial] to [target] in the main host. A tab switch is neither forward nor
 * back, so it fades, and so does one task giving way to the next, such as a lesson to the one after.
 */
internal fun StackTransitions.between(initial: Scene<NavKey>, target: Scene<NavKey>, back: Boolean): ContentTransform {
    val fromTask = initial.isTaskScreen()
    val toTask = target.isTaskScreen()
    return when {
        initial.tab() != target.tab() || (fromTask && toTask) -> fade
        toTask -> rise
        fromTask -> sink
        back -> pop
        else -> push
    }
}
