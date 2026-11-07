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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.R
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme

private object TopBarTokens {
    val MinHeight = 64.dp

    // An icon button is 48dp with a 24dp icon; 4dp outside it puts the icon on the 16dp keyline.
    val IconEdgePadding = 4.dp
}

/**
 * The bar at the top of a screen, on the paper colour. The title wraps to a second line before it
 * is shortened, so the bar grows at large font sizes. It pads itself for the status bar.
 *
 * With [onNavigateBack] it shows a back arrow labelled for screen readers. [actions] are icon
 * buttons drawn in the secondary text colour.
 */
@Composable
fun GilTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val spacing = GilTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .heightIn(min = TopBarTokens.MinHeight)
            .padding(
                start = if (onNavigateBack == null) spacing.screenMargin else TopBarTokens.IconEdgePadding,
                end = TopBarTokens.IconEdgePadding
            ),
        horizontalArrangement = Arrangement.spacedBy(TopBarTokens.IconEdgePadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onNavigateBack != null) {
            IconButton(onClick = onNavigateBack) {
                Icon(painter = GilIcons.ArrowBack, contentDescription = stringResource(R.string.common_navigate_back))
            }
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = spacing.itemGap)
                .padding(end = spacing.screenMargin - TopBarTokens.IconEdgePadding)
                .semantics { heading() },
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge
        )
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
            Row(content = actions)
        }
    }
}

@PreviewLightDark
@Composable
private fun GilTopBarPreview() {
    GilTheme {
        Surface {
            GilTopBar(title = "단어장", onNavigateBack = {})
        }
    }
}
