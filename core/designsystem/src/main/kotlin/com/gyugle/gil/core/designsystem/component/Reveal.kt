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
package com.gyugle.gil.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.gyugle.gil.core.designsystem.theme.GilTheme

/**
 * Fades [content] in over the reveal duration when it first appears, for what checking an answer
 * brings out. It leaves at once, since by then the screen is moving on.
 */
@Composable
fun Reveal(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val visible = remember { MutableTransitionState(false) }.apply { targetState = true }
    AnimatedVisibility(
        visibleState = visible,
        modifier = modifier,
        enter = fadeIn(tween(GilTheme.motion.reveal)),
        exit = ExitTransition.None
    ) {
        content()
    }
}
