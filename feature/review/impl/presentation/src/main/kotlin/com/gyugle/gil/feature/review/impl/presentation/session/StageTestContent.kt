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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.gyugle.gil.core.designsystem.component.PathMark
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.review.impl.presentation.R

/** What a stage test asks, what passing it does, and the button that starts it. */
@Composable
internal fun TestIntroContent(intro: TestIntroUi, onAction: (ReviewAction) -> Unit) {
    val stage = intro.stage.asString()
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollingBody(top = GilTheme.spacing.itemGap) {
            Title(stringResource(R.string.review_test_intro_title))
            Paragraph(
                pluralStringResource(R.plurals.review_test_intro_questions, intro.questions, stage, intro.questions)
            )
            Paragraph(
                pluralStringResource(R.plurals.review_test_intro_pass, intro.lessons, intro.passMark, intro.lessons)
            )
            Paragraph(stringResource(R.string.review_test_intro_after), muted = true)
        }
        BottomActions {
            PrimaryActionButton(
                text = stringResource(R.string.review_test_start),
                onClick = { onAction(ReviewAction.OnStartTestClick) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** How the test went: passed and what that recorded, or not and why, then what was missed. */
@Composable
internal fun TestResultContent(result: TestResultUi, onAction: (ReviewAction) -> Unit) {
    val spacing = GilTheme.spacing
    Column(modifier = Modifier.fillMaxSize()) {
        ScrollingBody(top = spacing.sectionGap) {
            PathMark()
            Title(
                if (result.passed) {
                    stringResource(R.string.review_test_passed_title, result.stage.asString())
                } else {
                    stringResource(R.string.review_test_failed_title)
                }
            )
            Paragraph(stringResource(R.string.review_result_score, result.right, result.answered))
            if (result.passed) {
                Paragraph(pluralStringResource(R.plurals.review_test_passed_lessons, result.lessons, result.lessons))
            } else {
                if (result.endedEarly) Paragraph(stringResource(R.string.review_test_failed_early))
                Paragraph(stringResource(R.string.review_test_failed_advice), muted = true)
            }
            if (result.missed.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.review_test_missed),
                    modifier = Modifier
                        .padding(top = spacing.itemGap)
                        .semantics { heading() },
                    style = MaterialTheme.typography.titleMedium
                )
                if (result.passed) Paragraph(stringResource(R.string.review_test_missed_comes_back), muted = true)
                Column {
                    result.missed.forEachIndexed { index, item ->
                        if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ResultItemRow(item)
                    }
                }
            }
        }
        BottomActions {
            PrimaryActionButton(
                text = stringResource(R.string.review_result_done),
                onClick = { onAction(ReviewAction.OnCloseClick) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ColumnScope.ScrollingBody(top: Dp, content: @Composable ColumnScope.() -> Unit) {
    val spacing = GilTheme.spacing
    Column(
        modifier = Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenMargin)
            .padding(top = top, bottom = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
        content = content
    )
}

@Composable
private fun Title(text: String) {
    Text(text = text, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun Paragraph(text: String, muted: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = if (muted) colors.onSurfaceVariant else colors.onSurface
    )
}
