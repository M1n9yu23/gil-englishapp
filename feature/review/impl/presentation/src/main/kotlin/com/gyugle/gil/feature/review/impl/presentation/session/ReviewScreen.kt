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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.EmptyState
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilProgressBar
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.feature.review.impl.presentation.R

private object ReviewScreenTokens {
    /** The bar row is one icon button tall. */
    val BarHeight = 48.dp

    // 4dp outside a 48dp icon button puts the close mark on the 16dp keyline, as in the top bar.
    val IconEdgePadding = 4.dp
}

@Composable
fun ReviewRoot(
    limit: Int,
    onClose: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel<ReviewViewModel, ReviewViewModel.Factory>(
        creationCallback = { factory -> factory.create(limit) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onAction(ReviewAction.OnResumed) }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onAction(ReviewAction.OnPaused) }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ReviewEvent.Close -> onClose()
        }
    }
    ReviewScreen(state = state, onAction = viewModel::onAction)
}

/**
 * A review session. Every answer is saved as it is given, so closing and back leave without asking.
 */
@Composable
fun ReviewScreen(state: ReviewState, onAction: (ReviewAction) -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val fade = GilTheme.motion.fade
            AnimatedContent(
                targetState = state.phase,
                // A new card or the result fades in; answering a card only changes its content.
                contentKey = { phase -> if (phase is ReviewPhase.Asking) "card-${phase.number}" else phase::class },
                transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) },
                label = "review phase"
            ) { phase ->
                when (phase) {
                    ReviewPhase.Loading -> Column {
                        CloseBar(onAction)
                        LoadingState()
                    }

                    ReviewPhase.Failed -> Column {
                        CloseBar(onAction)
                        ErrorState(
                            title = stringResource(R.string.review_error_title),
                            body = stringResource(R.string.review_error_body),
                            retryLabel = stringResource(R.string.review_error_retry),
                            onRetry = { onAction(ReviewAction.OnRetryClick) }
                        )
                    }

                    is ReviewPhase.Empty -> Column {
                        CloseBar(onAction)
                        EmptyState(
                            title = stringResource(R.string.review_empty_title),
                            body = nextReviewText(phase.nextInDays)
                                ?: stringResource(R.string.review_empty_no_cards),
                            actionLabel = stringResource(R.string.review_empty_action),
                            onAction = { onAction(ReviewAction.OnCloseClick) }
                        )
                    }

                    is ReviewPhase.Asking -> AskingContent(phase, onAction)

                    is ReviewPhase.Finished -> ResultContent(phase.result, onAction)
                }
            }
        }
    }
}

/** When the next review is, or null when the learner has no cards yet. */
@Composable
internal fun nextReviewText(nextInDays: Int?): String? = when (nextInDays) {
    null -> null
    1 -> stringResource(R.string.review_next_tomorrow)
    else -> pluralStringResource(R.plurals.review_next_in_days, nextInDays, nextInDays)
}

@Composable
private fun CloseBar(onAction: (ReviewAction) -> Unit, content: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = ReviewScreenTokens.BarHeight)
            .padding(start = ReviewScreenTokens.IconEdgePadding, end = GilTheme.spacing.screenMargin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
    ) {
        IconButton(onClick = { onAction(ReviewAction.OnCloseClick) }) {
            Icon(painter = GilIcons.Close, contentDescription = stringResource(R.string.review_close))
        }
        content()
    }
}

@Composable
private fun AskingContent(phase: ReviewPhase.Asking, onAction: (ReviewAction) -> Unit) {
    val spacing = GilTheme.spacing
    val description = stringResource(R.string.review_progress_description, phase.number, phase.count)
    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        CloseBar(onAction) {
            GilProgressBar(
                progress = (phase.number - 1).toFloat() / phase.count,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = description }
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenMargin)
                .padding(top = spacing.itemGap, bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap * 2)
        ) {
            Text(
                text = stringResource(phase.kind.instruction()),
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            CardContent(phase.card, phase.hasVoice, answered = phase.feedback != null, onAction = onAction)
            // In the scrolling part, so a long note at a large font never squeezes the card.
            phase.feedback?.let { feedback ->
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(feedback) { requester.bringIntoView() }
                FeedbackPanel(feedback, modifier = Modifier.bringIntoViewRequester(requester))
            }
        }
        BottomActions {
            when {
                phase.feedback != null -> PrimaryActionButton(
                    text = stringResource(R.string.review_continue),
                    onClick = { onAction(ReviewAction.OnContinueClick) },
                    modifier = Modifier.fillMaxWidth()
                )

                phase.card is CardUi.Typed -> PrimaryActionButton(
                    text = stringResource(R.string.review_check),
                    onClick = { onAction(ReviewAction.OnCheckClick) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = phase.canCheck
                )
            }
        }
    }
}

@Composable
internal fun BottomActions(content: @Composable ColumnScope.() -> Unit) {
    val spacing = GilTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenMargin)
            .padding(bottom = spacing.screenMargin),
        verticalArrangement = Arrangement.spacedBy(spacing.inlineGap),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content
    )
}

private fun CardKindUi.instruction(): Int = when (this) {
    CardKindUi.CHOOSE_MEANING -> R.string.review_instruction_meaning
    CardKindUi.CHOOSE_ENGLISH -> R.string.review_instruction_english
    CardKindUi.LISTEN_CHOOSE -> R.string.review_instruction_listen
    CardKindUi.CHOOSE_LETTER_BY_EAR -> R.string.review_instruction_letter_by_ear
    CardKindUi.CHOOSE_LETTER_BY_NAME -> R.string.review_instruction_letter_by_name
    CardKindUi.FIND_SOUND -> R.string.review_instruction_sound
    CardKindUi.SPELL_BY_EAR -> R.string.review_instruction_spell_by_ear
    CardKindUi.SPELL_BY_MEANING -> R.string.review_instruction_spell_by_meaning
    CardKindUi.FILL_BLANK -> R.string.review_instruction_blank
}

@PreviewLightDark
@Composable
private fun ReviewScreenChoicePreview() {
    GilTheme { ReviewScreen(state = ReviewPreviewData.meaningAnswered, onAction = {}) }
}

@Preview
@Composable
private fun ReviewScreenTypedPreview() {
    GilTheme { ReviewScreen(state = ReviewPreviewData.blankClose, onAction = {}) }
}

@Preview
@Composable
private fun ReviewScreenResultPreview() {
    GilTheme { ReviewScreen(state = ReviewPreviewData.finished, onAction = {}) }
}

@Preview
@Composable
private fun ReviewScreenEmptyPreview() {
    GilTheme { ReviewScreen(state = ReviewPreviewData.empty, onAction = {}) }
}
