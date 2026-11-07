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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.PressableSurface
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.dictionary.impl.presentation.R

private object ItemRowTokens {
    val MinHeight = 56.dp
}

/** A word, phrase or sound in a list: the English with its IPA, then its meaning. */
@Composable
internal fun ItemRow(item: ItemRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = GilTheme.spacing
    PressableSurface(
        onClick = onClick,
        clickLabel = stringResource(R.string.dictionary_open_entry),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = ItemRowTokens.MinHeight)
                .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
            horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
                    itemVerticalAlignment = Alignment.CenterVertically
                ) {
                    EnglishText(text = item.text)
                    // The word is read out already; its IPA would be read letter by letter.
                    item.ipa?.let { IpaText(ipa = it, spokenWord = null) }
                }
                val meaning =
                    item.meaningKo ?: if (item.isSound) stringResource(R.string.dictionary_entry_sound) else null
                meaning?.let {
                    KoreanProse(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item.stage?.let {
                    Text(
                        text = stringResource(R.string.dictionary_taught_in, it.asString()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item.mistakes?.let {
                Text(
                    text = pluralStringResource(R.plurals.dictionary_mistake_count, it, it),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun ItemRowPreview() {
    GilTheme {
        Surface {
            Column {
                ItemRow(ItemRowUi("sense.zero.num.count", "zero", "ˈzɪroʊ", "0, 영"), onClick = {})
                ItemRow(
                    ItemRowUi("word.thank", "thank", "θæŋk", "고마워하다", stage = LocalizedTextUi("일상", "Everyday life")),
                    onClick = {}
                )
                ItemRow(ItemRowUi("sound.f", "f, ph", "f", null, mistakes = 3, isSound = true), onClick = {})
            }
        }
    }
}
