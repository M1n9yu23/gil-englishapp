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
package com.gyugle.gil.feature.dictionary.impl.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.R as CoreR
import com.gyugle.gil.core.designsystem.theme.GilTheme

private object PlaceholderTokens {
    val BarHeight = 14.dp
    val RowMinHeight = 56.dp
    val RowFractions = listOf(0.35f, 0.5f, 0.3f, 0.45f)
}

/** Word list rows before they load: an English line over a meaning, read as one node. */
@Composable
internal fun ListLoading(modifier: Modifier = Modifier) {
    val spacing = GilTheme.spacing
    PlaceholderColumn(modifier) {
        PlaceholderTokens.RowFractions.forEachIndexed { index, fraction ->
            if (index > 0) HorizontalDivider()
            Column(
                modifier = Modifier
                    .heightIn(min = PlaceholderTokens.RowMinHeight)
                    .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap)
            ) {
                PlaceholderBar(fraction, MaterialTheme.typography.bodyLarge)
                PlaceholderBar(fraction * 0.7f, MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** A dictionary page before it loads: headword, pronunciation, then a section of meanings. */
@Composable
internal fun EntryLoading(modifier: Modifier = Modifier) {
    val spacing = GilTheme.spacing
    PlaceholderColumn(modifier.padding(horizontal = spacing.screenMargin)) {
        // About the line of the headword, which is set in the focus size of English text.
        PlaceholderBar(0.3f, MaterialTheme.typography.headlineSmall)
        PlaceholderBar(0.5f, MaterialTheme.typography.bodyLarge, Modifier.padding(top = spacing.inlineGap))
        PlaceholderBar(0.35f, MaterialTheme.typography.titleMedium, Modifier.padding(top = spacing.sectionGap))
        PlaceholderBar(0.25f, MaterialTheme.typography.titleMedium, Modifier.padding(top = spacing.itemGap))
        PlaceholderBar(0.7f, MaterialTheme.typography.bodyLarge, Modifier.padding(top = spacing.inlineGap))
        PlaceholderBar(0.55f, MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlaceholderColumn(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val label = stringResource(CoreR.string.common_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .clearAndSetSemantics { contentDescription = label }
    ) {
        content()
    }
}

/** A pale bar as tall as one line of [style], so the page does not jump when text replaces it. */
@Composable
private fun PlaceholderBar(widthFraction: Float, style: TextStyle, modifier: Modifier = Modifier) {
    val line = with(LocalDensity.current) { style.lineHeight.toDp() }
    Box(modifier = modifier.fillMaxWidth().height(line), contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(PlaceholderTokens.BarHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
        )
    }
}
