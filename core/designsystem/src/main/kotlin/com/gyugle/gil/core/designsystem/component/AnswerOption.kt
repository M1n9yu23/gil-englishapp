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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.gyugle.gil.core.designsystem.R
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.DeviceConfiguration
import com.gyugle.gil.core.presentation.currentDeviceConfiguration

private object AnswerOptionTokens {
    val MinHeight = 56.dp
    val BorderWidth = 1.dp
    val HorizontalPadding = 16.dp
    val VerticalPadding = 12.dp
    val MarkSize = 20.dp
}

/** How an answer looks once the question is answered. Before that every answer is [IDLE]. */
enum class AnswerMark { IDLE, CORRECT, PICKED_WRONG, OTHER }

/**
 * [count] options one under another; in a window too short to show them all that way, two to a
 * row with each pair the same height. [option] puts the given modifier on its [AnswerOption].
 */
@Composable
fun AnswerOptionList(
    count: Int,
    modifier: Modifier = Modifier,
    configuration: DeviceConfiguration = currentDeviceConfiguration(),
    option: @Composable (index: Int, modifier: Modifier) -> Unit
) {
    val gap = GilTheme.spacing.inlineGap
    val perRow = when (configuration) {
        DeviceConfiguration.MOBILE_LANDSCAPE -> 2

        DeviceConfiguration.MOBILE_PORTRAIT,
        DeviceConfiguration.TABLET_PORTRAIT,
        DeviceConfiguration.TABLET_LANDSCAPE,
        DeviceConfiguration.DESKTOP -> 1
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(gap)) {
        (0 until count).chunked(perRow).forEach { row ->
            Row(modifier = Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(gap)) {
                row.forEach { index -> option(index, Modifier.weight(1f).fillMaxHeight()) }
                // An odd last option keeps the width of the others.
                repeat(perRow - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * One answer to pick in a lesson step or a review. [english] draws [text] as English; [ipa], when
 * set, goes under it. After the answer the right one gets the ink wash and a check, the wrong pick
 * an underline and a "your choice" note, so neither depends on colour alone.
 */
@Composable
fun AnswerOption(
    text: String,
    english: Boolean,
    mark: AnswerMark,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ipa: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    val pickedLabel = stringResource(R.string.answer_option_picked)
    val correctLabel = stringResource(R.string.answer_option_correct)
    val background = if (mark == AnswerMark.CORRECT) colors.primaryContainer else colors.surface
    val content = when (mark) {
        AnswerMark.CORRECT -> colors.onPrimaryContainer
        AnswerMark.OTHER -> colors.onSurfaceVariant
        else -> colors.onSurface
    }
    PressableSurface(
        onClick = onClick,
        enabled = mark == AnswerMark.IDLE,
        shape = shape,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                when (mark) {
                    AnswerMark.PICKED_WRONG -> stateDescription = pickedLabel
                    AnswerMark.CORRECT -> stateDescription = correctLabel
                    else -> Unit
                }
            }
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = AnswerOptionTokens.MinHeight)
                .background(background, shape)
                .border(AnswerOptionTokens.BorderWidth, colors.outlineVariant, shape)
                .padding(
                    horizontal = AnswerOptionTokens.HorizontalPadding,
                    vertical = AnswerOptionTokens.VerticalPadding
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                val decoration = if (mark == AnswerMark.PICKED_WRONG) TextDecoration.Underline else null
                if (english) {
                    EnglishText(text = text, color = content, textDecoration = decoration)
                } else {
                    KoreanProse(
                        text = text,
                        color = content,
                        style = MaterialTheme.typography.bodyLarge.copy(textDecoration = decoration)
                    )
                }
                // The word itself is read just above.
                if (ipa != null) IpaText(ipa = ipa, spokenWord = null)
                if (mark == AnswerMark.PICKED_WRONG) {
                    Text(text = pickedLabel, style = MaterialTheme.typography.labelMedium, color = colors.error)
                }
            }
            if (mark == AnswerMark.CORRECT) {
                Icon(
                    painter = GilIcons.Check,
                    contentDescription = null,
                    tint = colors.onPrimaryContainer,
                    modifier = Modifier.size(AnswerOptionTokens.MarkSize)
                )
            }
        }
    }
}
