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
package com.gyugle.gil.feature.stats.impl.presentation.stats

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.EmptyState
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.designsystem.theme.tabularFigures
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.stats.impl.presentation.R

private object StatsTokens {
    val FigureRowMinHeight = 48.dp
}

@Composable
fun StatsRoot(onOpenLesson: (LessonId) -> Unit, viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsScreen(state = state, onAction = viewModel::onAction, onOpenLesson = onOpenLesson)
}

/**
 * The Stats tab: time and streak, the month's study days, the reviews ahead, how well the learned
 * items are known, and how far each stage has come.
 */
@Composable
fun StatsScreen(
    state: StatsState,
    onAction: (StatsAction) -> Unit,
    onOpenLesson: (LessonId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(R.string.stats_title))
        when (state) {
            StatsState.Loading -> LoadingState()

            StatsState.Failed -> ErrorState(
                title = stringResource(R.string.stats_error_title),
                body = stringResource(R.string.stats_error_body),
                retryLabel = stringResource(R.string.stats_error_retry),
                onRetry = { onAction(StatsAction.OnRetryClick) }
            )

            is StatsState.Empty -> {
                val first = state.firstLesson
                EmptyState(
                    title = stringResource(R.string.stats_empty_title),
                    body = stringResource(R.string.stats_empty_body),
                    actionLabel = first?.let { stringResource(R.string.stats_empty_start) },
                    onAction = first?.let { { onOpenLesson(it) } }
                )
            }

            is StatsState.Ready -> StatsContent(state.stats, onAction)
        }
    }
}

@Composable
private fun StatsContent(stats: StatsUi, onAction: (StatsAction) -> Unit) {
    val spacing = GilTheme.spacing
    // Lists pad their own rows; everything else keeps to the screen margin.
    val margin = Modifier.padding(horizontal = spacing.screenMargin)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = spacing.inlineGap, bottom = spacing.sectionGap),
        verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
    ) {
        Summary(stats)
        StudyCalendar(
            calendar = stats.calendar,
            onPrevious = { onAction(StatsAction.OnPreviousMonthClick) },
            onNext = { onAction(StatsAction.OnNextMonthClick) }
        )
        ReviewForecast(stats, margin)
        Levels(stats.levels)
        Stages(stats)
    }
}

@Composable
private fun Summary(stats: StatsUi) {
    Column {
        FigureRow(
            label = stringResource(R.string.stats_streak),
            value = pluralStringResource(R.plurals.stats_days, stats.streakDays, stats.streakDays)
        )
        RowDivider()
        FigureRow(label = stringResource(R.string.stats_this_week), value = studyTimeText(stats.weekMinutes))
        RowDivider()
        FigureRow(label = stringResource(R.string.stats_this_month), value = studyTimeText(stats.monthMinutes))
    }
}

/** A label and its figure on one line, read as one item. */
@Composable
private fun FigureRow(label: String, value: String) {
    val spacing = GilTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = StatsTokens.FigureRowMinHeight)
            .padding(horizontal = spacing.screenMargin, vertical = spacing.inlineGap)
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium.tabularFigures())
    }
}

@Composable
private fun Levels(levels: LevelsUi) {
    Column {
        SectionHeading(
            title = stringResource(R.string.stats_items_title),
            detail = pluralStringResource(R.plurals.stats_items_total, levels.total, levels.total),
            modifier = Modifier.padding(horizontal = GilTheme.spacing.screenMargin)
        )
        LevelRow(R.string.stats_level_learning, R.string.stats_level_learning_detail, levels.learning)
        RowDivider()
        LevelRow(R.string.stats_level_familiar, R.string.stats_level_familiar_detail, levels.familiar)
        RowDivider()
        LevelRow(R.string.stats_level_lasting, R.string.stats_level_lasting_detail, levels.lasting)
    }
}

@Composable
private fun LevelRow(@StringRes name: Int, @StringRes detail: Int, count: Int) {
    ListRow(
        headline = stringResource(name),
        supporting = stringResource(detail),
        trailing = {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleMedium.tabularFigures(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    )
}

@Composable
private fun Stages(stats: StatsUi) {
    Column {
        SectionHeading(
            title = stringResource(R.string.stats_stages_title),
            modifier = Modifier.padding(horizontal = GilTheme.spacing.screenMargin)
        )
        stats.stages.forEachIndexed { index, stage ->
            if (index > 0) RowDivider()
            ListRow(
                headline = stage.title.asString(),
                supporting = pluralStringResource(
                    R.plurals.stats_stage_lessons,
                    stage.total,
                    stage.completed,
                    stage.total
                ),
                trailing = if (stage.completed == stage.total) {
                    {
                        Icon(
                            painter = GilIcons.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    null
                }
            )
        }
    }
}

/**
 * A section title, with an optional short figure at the end of the line. The figure moves below
 * the title when both do not fit, so the title never wraps around it.
 */
@Composable
internal fun SectionHeading(title: String, modifier: Modifier = Modifier, detail: String? = null) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = title,
            // Keeps the figure apart when both only just fit on one line.
            modifier = Modifier
                .padding(end = GilTheme.spacing.itemGap)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium
        )
        if (detail != null) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = GilTheme.spacing.screenMargin),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

/** Whole minutes as the learner reads them: minutes under an hour, then hours and minutes. */
@Composable
private fun studyTimeText(minutes: Int): String {
    val hours = minutes / MINUTES_PER_HOUR
    val rest = minutes % MINUTES_PER_HOUR
    return when {
        hours == 0 -> stringResource(R.string.stats_minutes, rest)
        rest == 0 -> stringResource(R.string.stats_hours, hours)
        else -> stringResource(R.string.stats_hours_minutes, hours, rest)
    }
}

private const val MINUTES_PER_HOUR = 60

@PreviewLightDark
@Composable
private fun StatsScreenPreview() {
    GilTheme { Surface { StatsScreen(state = StatsPreviewData.ready, onAction = {}, onOpenLesson = {}) } }
}

@Preview
@Composable
private fun StatsScreenEmptyPreview() {
    GilTheme { Surface { StatsScreen(state = StatsPreviewData.empty, onAction = {}, onOpenLesson = {}) } }
}
