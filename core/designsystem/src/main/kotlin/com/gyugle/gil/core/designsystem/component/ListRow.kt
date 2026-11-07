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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme

internal val ListRowMinHeight = 56.dp

/**
 * One row of a list. Lists separate rows with a [HorizontalDivider], never with cards.
 *
 * With [onClick] the whole row is the touch target. [leading] and [trailing] are drawn in the
 * secondary text colour, so an icon there needs no tint of its own.
 */
@Composable
fun ListRow(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    if (onClick == null) {
        // Read as one item, like a clickable row, instead of stopping on each line.
        ListRowContent(headline, supporting, leading, trailing, modifier.semantics(mergeDescendants = true) {})
    } else {
        PressableSurface(onClick = onClick, modifier = modifier, clickLabel = clickLabel) {
            ListRowContent(headline, supporting, leading, trailing)
        }
    }
}

@Composable
private fun ListRowContent(
    headline: String,
    supporting: String?,
    leading: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
    val spacing = GilTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListRowMinHeight)
            .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            leading?.invoke()
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            trailing?.invoke()
        }
    }
}

@PreviewLightDark
@Composable
private fun ListRowPreview() {
    GilTheme {
        Surface {
            Column {
                ListRow(
                    headline = "인사와 소개",
                    supporting = "단어 12개, 문장 8개",
                    onClick = {},
                    trailing = { Icon(painter = GilIcons.ChevronRight, contentDescription = null) }
                )
                HorizontalDivider()
                ListRow(headline = "알림 시간", supporting = "오후 8:00")
            }
        }
    }
}
