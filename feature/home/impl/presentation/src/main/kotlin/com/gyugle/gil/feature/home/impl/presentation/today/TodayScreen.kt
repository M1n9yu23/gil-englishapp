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
package com.gyugle.gil.feature.home.impl.presentation.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilProgressBar
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.RowDivider
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.home.impl.presentation.R

@Composable
fun TodayRoot(
    onOpenReview: (limit: Int) -> Unit,
    onOpenLesson: (LessonId) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenListening: () -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    TodayScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenSettings = onOpenSettings,
        onOpenBlock = { block ->
            when (block) {
                is BlockUi.Review -> onOpenReview(block.count)
                is BlockUi.Lesson -> onOpenLesson(block.id)
                BlockUi.Listening -> onOpenListening()
                BlockUi.ReviewDone, BlockUi.CourseDone -> Unit
            }
        }
    )
}

/** The Today tab: how much of the goal is done, the plan in order, one button to go on, the week. */
@Composable
fun TodayScreen(
    state: TodayState,
    onAction: (TodayAction) -> Unit,
    onOpenBlock: (BlockUi) -> Unit,
    modifier: Modifier = Modifier,
    onOpenSettings: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(
            title = stringResource(R.string.home_title),
            actions = {
                IconButton(onClick = onOpenSettings) {
                    Icon(painter = GilIcons.Settings, contentDescription = stringResource(R.string.home_settings))
                }
            }
        )
        when (state) {
            TodayState.Loading -> LoadingState()

            TodayState.Failed -> ErrorState(
                title = stringResource(R.string.home_error_title),
                body = stringResource(R.string.home_error_body),
                retryLabel = stringResource(R.string.home_error_retry),
                onRetry = { onAction(TodayAction.OnRetryClick) }
            )

            is TodayState.Ready -> TodayContent(state.today, onOpenBlock)
        }
    }
}

@Composable
private fun TodayContent(today: TodayUi, onOpenBlock: (BlockUi) -> Unit) {
    val spacing = GilTheme.spacing
    Column(modifier = Modifier.fillMaxSize()) {
        // The plan's rows pad themselves; everything else keeps to the screen margin.
        val margin = Modifier.padding(horizontal = spacing.screenMargin)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.itemGap, bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.sectionGap)
        ) {
            GoalProgress(today.studiedMinutes, today.goalMinutes, margin)
            today.message?.let {
                KoreanProse(
                    text = stringResource(it.text()),
                    modifier = margin,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Blocks(today, onOpenBlock)
            WeekStrip(today, margin)
        }
        today.next?.let { next ->
            PrimaryActionButton(
                text = stringResource(
                    if (today.firstLesson) R.string.home_start_first_lesson else R.string.home_continue
                ),
                onClick = { onOpenBlock(next) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = spacing.screenMargin)
                    // A gap above, so text scrolling under never touches the button.
                    .padding(top = spacing.itemGap, bottom = spacing.screenMargin)
            )
        }
    }
}

@Composable
private fun GoalProgress(studied: Int, goal: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)) {
        // Side by side while both fit, the goal below at large font sizes.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            itemVerticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.home_studied_today, studied),
                // Keeps the goal apart when both only just fit on one line.
                modifier = Modifier.padding(end = GilTheme.spacing.itemGap),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.home_goal, goal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // The text above says the same.
        GilProgressBar(
            progress = studied.toFloat() / goal,
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {}
        )
    }
}

@Composable
private fun Blocks(today: TodayUi, onOpenBlock: (BlockUi) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        today.blocks.forEachIndexed { index, block ->
            if (index > 0) RowDivider()
            val chevron: @Composable () -> Unit = {
                Icon(painter = GilIcons.ChevronRight, contentDescription = null, tint = colors.onSurfaceVariant)
            }
            when (block) {
                is BlockUi.Review -> ListRow(
                    headline = stringResource(
                        if (block.more) R.string.home_block_more_review else R.string.home_block_review
                    ),
                    supporting = pluralStringResource(
                        R.plurals.home_block_review_size,
                        block.count,
                        block.count,
                        block.minutes
                    ),
                    onClick = { onOpenBlock(block) },
                    trailing = chevron
                )

                BlockUi.ReviewDone -> ListRow(
                    headline = stringResource(R.string.home_block_review),
                    supporting = stringResource(R.string.home_block_review_done),
                    trailing = { Icon(painter = GilIcons.Check, contentDescription = null, tint = colors.primary) }
                )

                is BlockUi.Lesson -> {
                    val detail =
                        stringResource(R.string.home_block_lesson_detail, block.unitTitle.asString(), block.minutes)
                    val enough = stringResource(R.string.home_block_lesson_enough)
                    ListRow(
                        headline = block.title.asString(),
                        supporting = if (block.enoughNewWords) "$detail\n$enough" else detail,
                        onClick = { onOpenBlock(block) },
                        trailing = chevron
                    )
                }

                BlockUi.CourseDone -> ListRow(
                    headline = stringResource(R.string.home_block_course_done),
                    supporting = stringResource(R.string.home_block_course_done_detail)
                )

                BlockUi.Listening -> ListRow(
                    headline = stringResource(R.string.home_block_listening),
                    supporting = stringResource(R.string.home_block_listening_detail),
                    onClick = { onOpenBlock(block) },
                    trailing = chevron
                )
            }
        }
    }
}

private fun TodayMessage.text(): Int = when (this) {
    TodayMessage.FIRST_DAY -> R.string.home_message_first_day
    TodayMessage.GOAL_MET -> R.string.home_message_goal_met
    TodayMessage.REVIEW_HEAVY -> R.string.home_message_review_heavy
}

@PreviewLightDark
@Composable
private fun TodayScreenPreview() {
    GilTheme { Surface { TodayScreen(state = TodayPreviewData.reviewsDue, onAction = {}, onOpenBlock = {}) } }
}

@Preview
@Composable
private fun TodayScreenFirstDayPreview() {
    GilTheme { Surface { TodayScreen(state = TodayPreviewData.firstDay, onAction = {}, onOpenBlock = {}) } }
}
