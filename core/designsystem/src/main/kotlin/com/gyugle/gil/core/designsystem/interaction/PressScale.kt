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
package com.gyugle.gil.core.designsystem.interaction

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import com.gyugle.gil.core.designsystem.theme.GilTheme

private const val PRESSED_SCALE = 0.97f

/**
 * Shrinks slightly while [interactionSource] is pressed. Returns a provider for [pressScale]: the
 * animation frames are read in the draw phase, so only press and release recompose the caller.
 */
@Composable
internal fun rememberPressScale(interactionSource: InteractionSource): () -> Float {
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = GilTheme.motion
    val scale = animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = tween(if (pressed) motion.pressIn else motion.pressOut),
        label = "pressScale"
    )
    return { scale.value }
}

internal fun Modifier.pressScale(scale: () -> Float): Modifier = graphicsLayer {
    val value = scale()
    scaleX = value
    scaleY = value
}
