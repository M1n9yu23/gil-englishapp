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
package com.gyugle.gil.feature.lesson.impl.presentation.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.PressableSurface
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.lesson.impl.presentation.R
import com.gyugle.gil.feature.lesson.impl.presentation.player.StepUi

private object TileBoardTokens {
    val BorderWidth = 1.dp
    val TilePaddingHorizontal = 12.dp
    val TilePaddingVertical = 6.dp

    /** Room for one row of tiles, so the board does not jump when the first tile lands. */
    val AnswerMinHeight = 64.dp
    val AnswerPadding = 8.dp

    /** A used tile stays as a faint gap, so the bank keeps its shape. */
    const val USED_ALPHA = 0.2f
}

/**
 * The sentence being built, above the bank of tiles. Tapping a bank tile places it; tapping a
 * placed tile takes it back. [enabled] is false once the answer is checked.
 */
@Composable
internal fun TileBoard(
    step: StepUi.Arrange,
    enabled: Boolean,
    onTileClick: (Int) -> Unit,
    onChosenTileClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    val bank = step.bank
    val chosen = step.chosen
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TileBoardTokens.AnswerMinHeight)
                .border(TileBoardTokens.BorderWidth, colors.outlineVariant, MaterialTheme.shapes.medium)
                .padding(TileBoardTokens.AnswerPadding),
            contentAlignment = Alignment.CenterStart
        ) {
            if (chosen.isEmpty()) {
                Text(
                    text = stringResource(R.string.lesson_tiles_empty),
                    modifier = Modifier.padding(horizontal = TileBoardTokens.TilePaddingHorizontal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap)) {
                chosen.forEachIndexed { position, bankIndex ->
                    val tile = bank[bankIndex]
                    Tile(
                        text = tile.text,
                        enabled = enabled,
                        clickLabel = stringResource(R.string.lesson_tile_remove, tile.text),
                        onClick = { onChosenTileClick(position) }
                    )
                }
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
            verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)
        ) {
            bank.forEachIndexed { index, tile ->
                val used = index in chosen
                Tile(
                    text = tile.text,
                    enabled = enabled && !used,
                    onClick = { onTileClick(index) },
                    modifier = if (used) Modifier.alpha(TileBoardTokens.USED_ALPHA) else Modifier
                )
            }
        }
    }
}

@Composable
private fun Tile(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    clickLabel: String? = null
) {
    val shape = MaterialTheme.shapes.small
    PressableSurface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        clickLabel = clickLabel,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .border(TileBoardTokens.BorderWidth, MaterialTheme.colorScheme.outline, shape)
                .padding(
                    horizontal = TileBoardTokens.TilePaddingHorizontal,
                    vertical = TileBoardTokens.TilePaddingVertical
                ),
            contentAlignment = Alignment.Center
        ) {
            EnglishText(text = text)
        }
    }
}
