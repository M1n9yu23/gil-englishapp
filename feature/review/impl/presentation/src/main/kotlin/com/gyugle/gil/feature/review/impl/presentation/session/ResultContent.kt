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
package com.gyugle.gil.feature.review.impl.presentation.session

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.PathMark
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.review.impl.presentation.R

/** The end of a session: how it went, what to look at again, and what is left. */
@Composable
internal fun ResultContent(result: ResultUi, onAction: (ReviewAction) -> Unit) {
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
            Text(
                text = stringResource(R.string.review_result_title),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall
            )
            Text(
                text = stringResource(R.string.review_result_score, result.right, result.answered),
                style = MaterialTheme.typography.bodyLarge
            )
            val next = if (result.remaining > 0) {
                pluralStringResource(R.plurals.review_result_remaining, result.remaining, result.remaining)
            } else {
                nextReviewText(result.nextInDays)
            }
            next?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge, color = colors.onSurfaceVariant) }
            if (result.again.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.review_result_again),
                    modifier = Modifier
                        .padding(top = spacing.itemGap)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium
                )
                Column {
                    result.again.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = colors.outlineVariant)
                        ResultItemRow(item)
                    }
                }
            }
        }
        BottomActions {
            if (result.remaining > 0) {
                PrimaryActionButton(
                    text = stringResource(R.string.review_result_keep_going),
                    onClick = { onAction(ReviewAction.OnKeepReviewingClick) },
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryActionButton(
                    text = stringResource(R.string.review_result_done),
                    onClick = { onAction(ReviewAction.OnCloseClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                PrimaryActionButton(
                    text = stringResource(R.string.review_result_done),
                    onClick = { onAction(ReviewAction.OnCloseClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** An item to look at again: its English, and its meaning or symbol at the end. */
@Composable
internal fun ResultItemRow(item: ResultItemUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GilTheme.spacing.itemGap)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
    ) {
        EnglishText(text = item.text, modifier = Modifier.weight(1f))
        // A meaning that wraps keeps the same right edge as the short ones.
        val trailing = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
            item.detail?.let {
                if (item.detailIsIpa) IpaText(ipa = it, spokenWord = null) else KoreanProse(text = it, style = trailing)
            }
        }
    }
}
