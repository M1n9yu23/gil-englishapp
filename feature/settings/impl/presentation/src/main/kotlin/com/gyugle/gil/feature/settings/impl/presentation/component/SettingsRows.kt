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
package com.gyugle.gil.feature.settings.impl.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.SectionTitle
import com.gyugle.gil.core.designsystem.icon.GilIcons

/** A titled group of rows. Rows reach the screen edges; the title keeps the margin. */
@Composable
internal fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionTitle(title)
        content()
    }
}

/** A row that opens another screen. */
@Composable
internal fun LinkRow(headline: String, onClick: () -> Unit, supporting: String? = null) {
    ListRow(
        headline = headline,
        supporting = supporting,
        onClick = onClick,
        trailing = { Icon(painter = GilIcons.ChevronRight, contentDescription = null) }
    )
}

/** A row that opens a choice in a dialog; [supporting] names the current choice. */
@Composable
internal fun ValueRow(headline: String, supporting: String, onClick: () -> Unit) {
    ListRow(headline = headline, supporting = supporting, onClick = onClick)
}
