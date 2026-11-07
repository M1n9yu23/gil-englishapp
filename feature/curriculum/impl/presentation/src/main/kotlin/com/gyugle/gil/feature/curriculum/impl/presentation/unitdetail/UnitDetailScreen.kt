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
package com.gyugle.gil.feature.curriculum.impl.presentation.unitdetail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.R as CoreR
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.PressableSurface
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.presentation.ContentErrorUi
import com.gyugle.gil.feature.curriculum.impl.presentation.R
import com.gyugle.gil.feature.curriculum.impl.presentation.component.PlaceholderBar

private object UnitDetailTokens {
    /** At least this wide, so one- and two-digit numbers keep the titles lined up. */
    val NumberWidth = 24.dp

    /** Margin, number and gap: where lesson titles start, in line with the top bar's title. */
    val TextStart = 56.dp
    val RowMinHeight = 56.dp
    const val PLACEHOLDER_ROWS = 4
}

@Composable
fun UnitDetailRoot(
    unitId: UnitId,
    onBack: () -> Unit,
    onLessonClick: (LessonId) -> Unit,
    viewModel: UnitDetailViewModel = hiltViewModel<UnitDetailViewModel, UnitDetailViewModel.Factory>(
        creationCallback = { factory -> factory.create(unitId.value) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UnitDetailScreen(state = state, onAction = viewModel::onAction, onBack = onBack, onLessonClick = onLessonClick)
}

@Composable
fun UnitDetailScreen(
    state: UnitDetailState,
    onAction: (UnitDetailAction) -> Unit,
    onBack: () -> Unit,
    onLessonClick: (LessonId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Empty while loading; the bar keeps its height so the list does not jump.
        GilTopBar(title = state.unit?.title?.asString().orEmpty(), onNavigateBack = onBack)
        Crossfade(
            targetState = state.content(),
            animationSpec = tween(GilTheme.motion.fade),
            label = "unit"
        ) { content ->
            when (content) {
                UnitDetailContent.Loading -> UnitLoading()
                is UnitDetailContent.Failed -> UnitError(content.error, onAction, onBack)
                is UnitDetailContent.Loaded -> LessonList(content.unit, onLessonClick)
            }
        }
    }
}

private sealed interface UnitDetailContent {
    data object Loading : UnitDetailContent
    data class Failed(val error: ContentErrorUi) : UnitDetailContent
    data class Loaded(val unit: UnitDetailUi) : UnitDetailContent
}

private fun UnitDetailState.content(): UnitDetailContent = when {
    error != null -> UnitDetailContent.Failed(error)
    isLoading || unit == null -> UnitDetailContent.Loading
    else -> UnitDetailContent.Loaded(unit)
}

/** A unit that is gone cannot come back by retrying, so that case offers the way back instead. */
@Composable
private fun UnitError(error: ContentErrorUi, onAction: (UnitDetailAction) -> Unit, onBack: () -> Unit) {
    ErrorState(
        title = error.title.asString(),
        body = error.body.asString(),
        retryLabel = stringResource(
            if (error.canRetry) R.string.curriculum_error_retry else R.string.curriculum_error_back
        ),
        onRetry = if (error.canRetry) ({ onAction(UnitDetailAction.OnRetryClick) }) else onBack
    )
}

@Composable
private fun LessonList(unit: UnitDetailUi, onLessonClick: (LessonId) -> Unit) {
    val spacing = GilTheme.spacing
    // A pushed screen has no bottom bar, so it keeps its last row clear of the navigation bar.
    val navigationBar = WindowInsets.navigationBars.asPaddingValues()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = spacing.sectionGap + navigationBar.calculateBottomPadding())
    ) {
        // The stage this unit belongs to, on the same edge as the bar's title and the lesson titles.
        item(key = "stage") {
            Text(
                text = unit.stageTitle.asString(),
                modifier = Modifier.padding(
                    start = UnitDetailTokens.TextStart,
                    end = spacing.screenMargin,
                    bottom = spacing.inlineGap
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        itemsIndexed(unit.lessons, key = { _, lesson -> lesson.id.value }) { index, lesson ->
            if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = UnitDetailTokens.TextStart))
            LessonRow(lesson, onClick = { onLessonClick(lesson.id) })
        }
    }
}

/** A lesson with its number on the first line of the title, however far the goal wraps. */
@Composable
private fun LessonRow(lesson: LessonRowUi, onClick: () -> Unit) {
    PressableSurface(
        onClick = onClick,
        clickLabel = stringResource(R.string.curriculum_lesson_open),
        modifier = Modifier.fillMaxWidth()
    ) {
        LessonRowContent(lesson)
    }
}

@Composable
private fun LessonRowContent(lesson: LessonRowUi) {
    val spacing = GilTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = UnitDetailTokens.RowMinHeight)
            .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        Text(
            text = lesson.number.toString(),
            modifier = Modifier
                .widthIn(min = UnitDetailTokens.NumberWidth)
                .alignByBaseline(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Column(modifier = Modifier.weight(1f).alignBy(FirstBaseline)) {
            Text(
                text = lesson.title.asString(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = lesson.goal.asString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** The lesson list's shape before it loads: a number and two lines per row, read as one node. */
@Composable
private fun UnitLoading() {
    val spacing = GilTheme.spacing
    val label = stringResource(CoreR.string.common_loading)
    val density = LocalDensity.current
    val titleLine = with(density) { MaterialTheme.typography.bodyLarge.lineHeight.toDp() }
    val goalLine = with(density) { MaterialTheme.typography.bodyMedium.lineHeight.toDp() }
    Column(modifier = Modifier.fillMaxSize().clearAndSetSemantics { contentDescription = label }) {
        PlaceholderBar(
            widthFraction = 0.3f,
            lineHeight = with(density) { MaterialTheme.typography.labelLarge.lineHeight.toDp() },
            modifier = Modifier.padding(start = UnitDetailTokens.TextStart, bottom = spacing.inlineGap)
        )
        repeat(UnitDetailTokens.PLACEHOLDER_ROWS) { index ->
            if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = UnitDetailTokens.TextStart))
            Column(
                modifier = Modifier
                    .heightIn(min = UnitDetailTokens.RowMinHeight)
                    .padding(
                        start = UnitDetailTokens.TextStart,
                        end = spacing.screenMargin,
                        top = spacing.itemGap,
                        bottom = spacing.itemGap
                    )
            ) {
                PlaceholderBar(widthFraction = 0.45f, lineHeight = titleLine)
                PlaceholderBar(widthFraction = 0.7f, lineHeight = goalLine)
            }
        }
    }
}

@PreviewLightDark
@Composable
private fun UnitDetailScreenPreview() {
    GilTheme {
        Surface {
            UnitDetailScreen(
                state = UnitDetailState(isLoading = false, unit = UnitDetailPreviewData.unit),
                onAction = {},
                onBack = {},
                onLessonClick = {}
            )
        }
    }
}
