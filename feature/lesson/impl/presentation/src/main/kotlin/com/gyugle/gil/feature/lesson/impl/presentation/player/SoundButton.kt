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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.icon.GilIcons

private object SoundButtonTokens {
    /** The one sound button a listening step is about, easy to hit again and again. */
    val LargeSize = 64.dp
    val LargeIconSize = 32.dp
}

/**
 * Plays a sound. [large] is the main button of a listening step; the small one sits next to a
 * sentence. [description] names what plays, for screen readers.
 */
@Composable
internal fun SoundButton(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    large: Boolean = false
) {
    if (large) {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.size(SoundButtonTokens.LargeSize),
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                painter = GilIcons.VolumeUp,
                contentDescription = description,
                modifier = Modifier.size(SoundButtonTokens.LargeIconSize)
            )
        }
    } else {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(
                painter = GilIcons.VolumeUp,
                contentDescription = description,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
