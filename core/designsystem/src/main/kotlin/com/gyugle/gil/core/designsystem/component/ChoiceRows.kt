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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.gyugle.gil.core.designsystem.theme.GilTheme

/** The title of a group of rows, kept to the screen margin while the rows reach the edges. */
@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        modifier = modifier
            .padding(horizontal = GilTheme.spacing.screenMargin)
            .padding(bottom = GilTheme.spacing.inlineGap)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** The line between rows of one group, inset to the screen margin. */
@Composable
fun RowDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = GilTheme.spacing.screenMargin),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** One choice of several. The row is the touch target and carries the state; the button only draws it. */
@Composable
fun ChoiceRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    horizontalPadding: Dp = GilTheme.spacing.screenMargin
) {
    val spacing = GilTheme.spacing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListRowMinHeight)
            .selectable(selected = selected, enabled = enabled, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = horizontalPadding, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowTexts(label, supporting, enabled, Modifier.weight(1f))
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(unselectedColor = MaterialTheme.colorScheme.outline)
        )
    }
}

/** An on and off setting, with the switch drawn at the end and the whole row as its target. */
@Composable
fun SwitchRow(
    headline: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null
) {
    val spacing = GilTheme.spacing
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = ListRowMinHeight)
            .toggleable(value = checked, role = Role.Switch) { on ->
                haptics.performHapticFeedback(if (on) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
                onCheckedChange(on)
            }
            .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RowTexts(headline, supporting, enabled = true, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(uncheckedBorderColor = MaterialTheme.colorScheme.outline)
        )
    }
}

@Composable
private fun RowTexts(headline: String, supporting: String?, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = headline,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) colors.onSurface else colors.onSurfaceVariant
        )
        if (supporting != null) {
            Text(text = supporting, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

/** A choice in a dialog. [options] are [ChoiceRow]s without horizontal padding; picking one closes it. */
@Composable
fun ChoiceDialog(
    title: String,
    dismissLabel: String,
    onDismiss: () -> Unit,
    options: @Composable ColumnScope.() -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState()),
                content = options
            )
        },
        confirmButton = { TextActionButton(text = dismissLabel, onClick = onDismiss) }
    )
}
