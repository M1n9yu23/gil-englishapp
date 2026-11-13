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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.PathMark
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.lesson.impl.presentation.R

/** The end of a lesson: time spent, the lesson's goal, what it introduced, and where to go next. */
@Composable
internal fun ResultContent(title: LocalizedTextUi?, result: ResultUi, onAction: (LessonAction) -> Unit) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenMargin)
                .padding(top = spacing.sectionGap, bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            PathMark()
            title?.let {
                Text(text = it.asString(), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
            Text(
                text = stringResource(R.string.lesson_result_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = pluralStringResource(R.plurals.lesson_result_minutes, result.minutes, result.minutes),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )
            Text(text = result.goal.asString(), style = MaterialTheme.typography.bodyLarge)
            if (result.items.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.lesson_result_items),
                    modifier = Modifier
                        .padding(top = spacing.itemGap)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium
                )
                Column {
                    result.items.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                        ResultItemRow(item)
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenMargin)
                .padding(bottom = spacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(spacing.inlineGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (result.saveFailed) {
                Text(
                    text = stringResource(R.string.lesson_result_save_failed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.error
                )
                TextActionButton(text = stringResource(R.string.lesson_result_retry), onClick = {
                    onAction(LessonAction.OnSaveRetryClick)
                })
            }
            if (result.hasNext) {
                PrimaryActionButton(
                    text = stringResource(R.string.lesson_result_next),
                    onClick = { onAction(LessonAction.OnNextLessonClick) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !result.isSaving
                )
                SecondaryActionButton(
                    text = stringResource(R.string.lesson_result_done),
                    onClick = { onAction(LessonAction.OnDoneClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PrimaryActionButton(
                    text = stringResource(R.string.lesson_result_done),
                    onClick = { onAction(LessonAction.OnDoneClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun ResultItemRow(item: ResultItemUi) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GilTheme.spacing.itemGap)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
    ) {
        // The meaning or note lines up with the word, not with the top of the row.
        Column(modifier = Modifier.weight(1f).alignByBaseline()) {
            EnglishText(text = item.text)
            // The row is read as one, and the word is already in it.
            item.ipa?.let { IpaText(ipa = it, spokenWord = null) }
        }
        // A meaning that wraps keeps the same right edge as the short ones.
        val trailing = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End)
        Column(modifier = Modifier.weight(1f).alignByBaseline(), horizontalAlignment = Alignment.End) {
            item.meaningKo?.let { KoreanProse(text = it, style = trailing) }
            if (item.comesBackSoon) {
                Text(
                    text = stringResource(R.string.lesson_result_comes_back),
                    style = trailing,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
