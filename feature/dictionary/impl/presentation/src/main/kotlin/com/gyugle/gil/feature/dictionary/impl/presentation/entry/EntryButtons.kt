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
package com.gyugle.gil.feature.dictionary.impl.presentation.entry

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.component.SoundButton
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.feature.dictionary.impl.presentation.R

private object EntryButtonTokens {
    val AccentIconSize = 18.dp
    val AccentIconGap = 6.dp
    val AccentPadding = PaddingValues(start = 8.dp, end = 12.dp)
}

/** Plays the headword in one accent; the label names the accent, so the icon is not read. */
@Composable
internal fun AccentButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        contentPadding = EntryButtonTokens.AccentPadding
    ) {
        Icon(
            painter = GilIcons.VolumeUp,
            contentDescription = null,
            modifier = Modifier.size(EntryButtonTokens.AccentIconSize)
        )
        Spacer(Modifier.width(EntryButtonTokens.AccentIconGap))
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

/** Plays [text], which screen readers name with the button. */
@Composable
internal fun PlayButton(text: String, onClick: () -> Unit) {
    SoundButton(onClick = onClick, description = stringResource(R.string.dictionary_play_text, text))
}

/**
 * Puts an item in the word list or takes it out; the bookmark fills while it is in. [name] is what
 * screen readers say it saves, such as the meaning beside it.
 */
@Composable
internal fun SaveToggle(name: String, saved: Boolean, onToggle: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val state = stringResource(if (saved) R.string.dictionary_saved else R.string.dictionary_not_saved)
    IconToggleButton(
        checked = saved,
        onCheckedChange = { checked ->
            haptics.performHapticFeedback(if (checked) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)
            onToggle()
        },
        modifier = Modifier.semantics { stateDescription = state }
    ) {
        Icon(
            painter = if (saved) GilIcons.BookmarkFilled else GilIcons.Bookmark,
            contentDescription = stringResource(R.string.dictionary_save, name),
            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
