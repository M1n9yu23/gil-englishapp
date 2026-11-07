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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.interaction.pressScale
import com.gyugle.gil.core.designsystem.interaction.rememberConfirmClick
import com.gyugle.gil.core.designsystem.interaction.rememberPressScale
import com.gyugle.gil.core.designsystem.theme.GilTheme

private object ActionButtonTokens {
    val Padding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    val TextPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    val BorderWidth = 1.dp
}

/**
 * The one action a screen is for: "Continue", "Check". At most one per screen. Confirms with a
 * haptic, because pressing it commits something.
 */
@Composable
fun PrimaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    Button(
        onClick = rememberConfirmClick(onClick),
        modifier = modifier.pressScale(scale),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        // The default disabled fill (onSurface at 12%) reads cold on paper.
        colors = ButtonDefaults.buttonColors(disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = null,
        contentPadding = ActionButtonTokens.Padding,
        interactionSource = interactionSource
    ) {
        ButtonLabel(text, MaterialTheme.typography.titleMedium)
    }
}

/** An alternative next to the primary action: "Play again", "Skip". */
@Composable
fun SecondaryActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val interactionSource = remember { MutableInteractionSource() }
    val scale = rememberPressScale(interactionSource)
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(scale),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        border = BorderStroke(
            width = ActionButtonTokens.BorderWidth,
            color = if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant
        ),
        contentPadding = ActionButtonTokens.Padding,
        interactionSource = interactionSource
    ) {
        ButtonLabel(text, MaterialTheme.typography.titleMedium)
    }
}

/** A minor action inside content or a bar: "Report a mistake", "Reload". */
@Composable
fun TextActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
        contentPadding = ActionButtonTokens.TextPadding
    ) {
        ButtonLabel(text, MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun ButtonLabel(text: String, style: TextStyle) {
    // No maxLines: at large font sizes the label wraps instead of being cut.
    Text(text = text, style = style, textAlign = TextAlign.Center)
}

@PreviewLightDark
@Composable
private fun ActionButtonsPreview() {
    GilTheme {
        Surface {
            Column(
                modifier = Modifier.padding(GilTheme.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
            ) {
                PrimaryActionButton(text = "이어서 하기", onClick = {}, modifier = Modifier.fillMaxWidth())
                PrimaryActionButton(text = "확인", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
                SecondaryActionButton(text = "다시 듣기", onClick = {}, modifier = Modifier.fillMaxWidth())
                TextActionButton(text = "오류 신고", onClick = {})
            }
        }
    }
}
