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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.PressableSurface
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.lesson.impl.presentation.R
import com.gyugle.gil.feature.lesson.impl.presentation.player.OptionUi

private object ChoiceOptionTokens {
    val MinHeight = 56.dp
    val BorderWidth = 1.dp
    val HorizontalPadding = 16.dp
    val VerticalPadding = 12.dp
    val MarkSize = 20.dp
}

/** How an option looks once the step is answered. Before that every option is [IDLE]. */
internal enum class OptionMark { IDLE, CORRECT, PICKED_WRONG, OTHER }

/**
 * One answer to pick. After the answer the right one gets the ink wash and a check, the wrong pick
 * an underline and a "your choice" note, so neither depends on colour alone.
 */
@Composable
internal fun ChoiceOption(option: OptionUi, mark: OptionMark, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val pickedLabel = stringResource(R.string.lesson_choice_picked)
    val background = if (mark == OptionMark.CORRECT) colors.primaryContainer else colors.surface
    val content = when (mark) {
        OptionMark.CORRECT -> colors.onPrimaryContainer
        OptionMark.OTHER -> colors.onSurfaceVariant
        else -> colors.onSurface
    }
    PressableSurface(
        onClick = onClick,
        enabled = mark == OptionMark.IDLE,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .semantics { if (mark == OptionMark.PICKED_WRONG) stateDescription = pickedLabel }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = ChoiceOptionTokens.MinHeight)
                .background(background, shape)
                .border(ChoiceOptionTokens.BorderWidth, colors.outlineVariant, shape)
                .padding(
                    horizontal = ChoiceOptionTokens.HorizontalPadding,
                    vertical = ChoiceOptionTokens.VerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val decoration = if (mark == OptionMark.PICKED_WRONG) TextDecoration.Underline else null
                if (option.english) {
                    EnglishText(text = option.text, color = content, textDecoration = decoration)
                } else {
                    KoreanProse(
                        text = option.text,
                        color = content,
                        style = MaterialTheme.typography.bodyLarge.copy(textDecoration = decoration)
                    )
                }
                if (option.ipa != null) IpaText(ipa = option.ipa, spokenWord = option.text)
                if (mark == OptionMark.PICKED_WRONG) {
                    Text(text = pickedLabel, style = MaterialTheme.typography.labelMedium, color = colors.error)
                }
            }
            if (mark == OptionMark.CORRECT) {
                Icon(
                    painter = GilIcons.Check,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(ChoiceOptionTokens.MarkSize)
                )
            }
        }
    }
}
