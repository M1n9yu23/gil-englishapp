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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.R
import com.gyugle.gil.core.designsystem.theme.GilTheme

// Short state messages read better as even lines than as one full line and a lone word.
private val BalancedLineBreak = LineBreak.Heading

private object StateTokens {
    val PlaceholderTitleHeight = 24.dp
    val PlaceholderHeadlineHeight = 16.dp
    val PlaceholderSupportingHeight = 12.dp
    val PlaceholderRowFractions = listOf(0.6f, 0.45f, 0.55f)

    // The motif's canvas already leaves air above the title; a full section gap looked detached.
    val MotifToTitle = 24.dp
}

/**
 * Placeholder blocks shaped like a title over [ListRow]s, so the screen does not jump when the
 * content arrives. It does not animate; local data loads fast enough that a moving placeholder
 * would only flash.
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    val spacing = GilTheme.spacing
    val label = stringResource(R.string.common_loading)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = spacing.sectionGap)
            .clearAndSetSemantics { contentDescription = label }
    ) {
        PlaceholderBlock(
            height = StateTokens.PlaceholderTitleHeight,
            widthFraction = 0.5f,
            modifier = Modifier.padding(horizontal = spacing.screenMargin)
        )
        Spacer(Modifier.height(spacing.sectionGap))
        StateTokens.PlaceholderRowFractions.forEachIndexed { index, fraction ->
            if (index > 0) HorizontalDivider()
            PlaceholderRow(fraction)
        }
    }
}

/** Same height as a two-line [ListRow] at any font scale: each bar sits in a box one text line tall. */
@Composable
private fun PlaceholderRow(widthFraction: Float) {
    val typography = MaterialTheme.typography
    val density = LocalDensity.current
    val headlineLine = with(density) { typography.bodyLarge.lineHeight.toDp() }
    val supportingLine = with(density) { typography.bodyMedium.lineHeight.toDp() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ListRowMinHeight)
            .padding(horizontal = GilTheme.spacing.screenMargin, vertical = GilTheme.spacing.itemGap)
    ) {
        Box(modifier = Modifier.height(headlineLine), contentAlignment = Alignment.CenterStart) {
            PlaceholderBlock(height = StateTokens.PlaceholderHeadlineHeight, widthFraction = widthFraction)
        }
        Box(modifier = Modifier.height(supportingLine), contentAlignment = Alignment.CenterStart) {
            PlaceholderBlock(height = StateTokens.PlaceholderSupportingHeight, widthFraction = widthFraction * 0.7f)
        }
    }
}

/**
 * Nothing to show yet. [body] tells the learner what to do next, and [actionLabel] with [onAction]
 * gives them the button for it; the button appears only when both are given.
 */
@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    StateLayout(modifier) {
        PathMark()
        Spacer(Modifier.height(StateTokens.MotifToTitle))
        StateText(title, body)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(GilTheme.spacing.sectionGap))
            PrimaryActionButton(text = actionLabel, onClick = onAction)
        }
    }
}

/**
 * Something failed. Say what happened in [title] and what the learner can do in [body]; there is
 * no red and no alarm icon, because most failures here are a retry away.
 */
@Composable
fun ErrorState(title: String, body: String, retryLabel: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    StateLayout(modifier) {
        StateText(title, body)
        Spacer(Modifier.height(GilTheme.spacing.sectionGap))
        SecondaryActionButton(text = retryLabel, onClick = onRetry)
    }
}

/** Left-aligned, vertically centred, and scrollable so that large font sizes never cut the text. */
@Composable
private fun StateLayout(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .fillMaxWidth()
                .padding(horizontal = GilTheme.spacing.screenMargin, vertical = GilTheme.spacing.sectionGap),
            verticalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
private fun StateText(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(lineBreak = BalancedLineBreak),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge.copy(lineBreak = BalancedLineBreak),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PlaceholderBlock(height: Dp, widthFraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
    )
}

@PreviewLightDark
@Composable
private fun EmptyStatePreview() {
    GilTheme {
        Surface {
            EmptyState(
                title = "아직 저장한 단어가 없어요",
                body = "과를 공부하다가 단어를 눌러 단어장에 넣어 보세요.",
                actionLabel = "오늘 공부 시작하기",
                onAction = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ErrorStatePreview() {
    GilTheme {
        Surface {
            ErrorState(
                title = "학습 기록을 불러오지 못했어요",
                body = "휴대폰 저장 공간이 부족하면 이럴 수 있어요.",
                retryLabel = "다시 불러오기",
                onRetry = {}
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun LoadingStatePreview() {
    GilTheme {
        Surface { LoadingState() }
    }
}
