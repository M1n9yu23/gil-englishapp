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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.theme.GilTheme

private object ProgressBarTokens {
    val Height = 12.dp
}

/**
 * How far along a lesson or unit is, drawn as the path motif: an ink line over a pale track with a
 * seal dot where the learner is now. [progress] is clamped to 0..1.
 *
 * The pale track is too faint to carry meaning on its own, so a screen shows the count in text as
 * well ("3/8"); the bar is the visual summary of that number.
 */
@Composable
fun GilProgressBar(progress: Float, modifier: Modifier = Modifier) {
    val clamped = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = clamped,
        animationSpec = tween(GilTheme.motion.fade),
        label = "progress"
    )
    val track = MaterialTheme.colorScheme.primaryContainer
    val ink = MaterialTheme.colorScheme.primary
    val seal = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(ProgressBarTokens.Height)
            .progressSemantics(clamped)
    ) {
        val radius = PathMotif.SealRadius.toPx()
        val stroke = PathMotif.Stroke.toPx()
        val y = size.height / 2
        val length = size.width - radius * 2
        fun x(fraction: Float): Float {
            val ltr = radius + length * fraction
            return if (layoutDirection == LayoutDirection.Rtl) size.width - ltr else ltr
        }
        drawLine(track, Offset(x(0f), y), Offset(x(1f), y), stroke, StrokeCap.Round)
        if (animated > 0f) {
            drawLine(ink, Offset(x(0f), y), Offset(x(animated), y), stroke, StrokeCap.Round)
        }
        drawCircle(seal, radius, Offset(x(animated), y))
    }
}

@PreviewLightDark
@Composable
private fun GilProgressBarPreview() {
    GilTheme {
        Surface {
            Column(
                modifier = Modifier.padding(GilTheme.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
            ) {
                GilProgressBar(progress = 0f)
                GilProgressBar(progress = 0.4f)
                GilProgressBar(progress = 1f)
            }
        }
    }
}
