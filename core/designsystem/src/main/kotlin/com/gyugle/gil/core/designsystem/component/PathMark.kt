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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** One proportion for every drawing of the path motif, so the line and its seal dot match. */
internal object PathMotif {
    val Stroke = 3.dp
    val SealRadius = 4.dp
}

private object PathMarkTokens {
    val Width = 96.dp
    val Height = 32.dp
}

/**
 * Gil's one motif, in place of illustrations: a single ink line that bends and moves forward, with
 * a seal-red dot where it ends. Decorative, so it has no semantics. It opens the empty states and
 * the first screen of the app, above the title.
 */
@Composable
fun PathMark(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val seal = MaterialTheme.colorScheme.tertiary
    Canvas(modifier = modifier.size(PathMarkTokens.Width, PathMarkTokens.Height)) {
        val stroke = PathMotif.Stroke.toPx()
        val radius = PathMotif.SealRadius.toPx()
        val start = Offset(stroke / 2, size.height - radius)
        val end = Offset(size.width - radius, radius)
        val path = Path().apply {
            moveTo(start.x, start.y)
            cubicTo(size.width * 0.5f, start.y, size.width * 0.45f, end.y, end.x, end.y)
        }
        drawPath(path = path, color = ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawCircle(color = seal, radius = radius, center = end)
    }
}
