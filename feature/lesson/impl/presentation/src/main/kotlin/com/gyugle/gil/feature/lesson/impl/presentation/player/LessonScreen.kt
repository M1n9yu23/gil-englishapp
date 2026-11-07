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

import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilProgressBar
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.core.presentation.permissions.Permission
import com.gyugle.gil.core.presentation.permissions.rememberPermissionController
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.lesson.impl.presentation.R
import com.gyugle.gil.feature.lesson.impl.presentation.player.FeedbackPanel
import com.gyugle.gil.feature.lesson.impl.presentation.player.KoreanProse
import kotlinx.coroutines.launch

private object LessonScreenTokens {
    /** The bar row is one icon button tall. */
    val BarHeight = 48.dp

    // 4dp outside a 48dp icon button puts the close mark on the 16dp keyline, as in the top bar.
    val IconEdgePadding = 4.dp
}

@Composable
fun LessonRoot(
    lessonId: LessonId,
    onClose: () -> Unit,
    onOpenLesson: (LessonId) -> Unit,
    viewModel: LessonViewModel = hiltViewModel<LessonViewModel, LessonViewModel.Factory>(
        creationCallback = { factory -> factory.create(lessonId.value) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberPermissionController()
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(LessonAction.OnMicrophoneState(permissions.getPermissionState(Permission.MICROPHONE)))
        viewModel.onAction(LessonAction.OnResumed)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { viewModel.onAction(LessonAction.OnPaused) }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LessonEvent.Close -> onClose()

            is LessonEvent.OpenLesson -> onOpenLesson(event.lessonId)

            LessonEvent.RequestMicrophone -> scope.launch {
                viewModel.onAction(LessonAction.OnMicrophoneState(permissions.requestPermission(Permission.MICROPHONE)))
            }

            LessonEvent.OpenAppSettings -> permissions.openAppSettings()
        }
    }
    LessonScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun LessonScreen(state: LessonState, onAction: (LessonAction) -> Unit, modifier: Modifier = Modifier) {
    BackHandler(enabled = state.phase is LessonPhase.Playing) { onAction(LessonAction.OnCloseClick) }
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            val fade = GilTheme.motion.fade
            AnimatedContent(
                targetState = state.phase,
                // A new step or the result fades in; answering within a step only changes the content.
                contentKey = { phase ->
                    when (phase) {
                        is LessonPhase.Playing -> "step-${phase.stepNumber}"
                        else -> phase::class
                    }
                },
                transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) },
                label = "lesson phase"
            ) { phase ->
                when (phase) {
                    LessonPhase.Loading -> Column {
                        CloseBar(onAction)
                        LoadingState()
                    }

                    is LessonPhase.Failed -> Column {
                        CloseBar(onAction)
                        val canRetry = phase.error.canRetry
                        ErrorState(
                            title = phase.error.title.asString(),
                            body = phase.error.body.asString(),
                            retryLabel = stringResource(
                                if (canRetry) R.string.lesson_error_retry else R.string.lesson_error_back
                            ),
                            onRetry = {
                                onAction(if (canRetry) LessonAction.OnRetryLoadClick else LessonAction.OnCloseClick)
                            }
                        )
                    }

                    is LessonPhase.AskResume -> ResumeContent(state.title, phase, onAction)

                    is LessonPhase.Playing -> PlayingContent(phase, onAction)

                    is LessonPhase.Finished -> ResultContent(state.title, phase.result, onAction)
                }
            }
        }
    }
    if (state.isExitConfirmShown) ExitDialog(onAction)
}

@Composable
private fun CloseBar(onAction: (LessonAction) -> Unit, content: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = LessonScreenTokens.BarHeight)
            .padding(start = LessonScreenTokens.IconEdgePadding, end = GilTheme.spacing.screenMargin),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
    ) {
        IconButton(onClick = { onAction(LessonAction.OnCloseClick) }) {
            Icon(painter = GilIcons.Close, contentDescription = stringResource(R.string.lesson_close))
        }
        content()
    }
}

/** A scrolling body and actions kept at the bottom, above the keyboard. */
@Composable
private fun StepFrame(
    body: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit
) {
    val spacing = GilTheme.spacing
    Column(modifier = modifier.fillMaxSize().imePadding()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenMargin)
                .padding(top = spacing.itemGap, bottom = spacing.sectionGap),
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap * 2)
        ) {
            body()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenMargin)
                .padding(bottom = spacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(spacing.inlineGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            actions()
        }
    }
}

@Composable
private fun PlayingContent(phase: LessonPhase.Playing, onAction: (LessonAction) -> Unit) {
    val description = stringResource(R.string.lesson_progress_description, phase.stepNumber, phase.stepCount)
    Column {
        CloseBar(onAction) {
            GilProgressBar(
                progress = (phase.stepNumber - 1).toFloat() / phase.stepCount,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = description }
            )
        }
        val answered = phase.feedback != null
        StepFrame(
            body = {
                if (phase.showsNoVoiceNotice) {
                    KoreanProse(
                        text = stringResource(R.string.lesson_no_voice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (phase.isAgain) {
                    Text(
                        text = stringResource(R.string.lesson_again_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                phase.step.instruction()?.let {
                    Text(
                        text = it,
                        modifier = Modifier.semantics { heading() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StepContent(phase.step, phase.hasVoice, answered, onAction)
                // In the scrolling part, so a long reason at a large font never squeezes the step;
                // it scrolls into view when it appears.
                phase.feedback?.let { feedback ->
                    val requester = remember { BringIntoViewRequester() }
                    LaunchedEffect(feedback) { requester.bringIntoView() }
                    FeedbackPanel(feedback, modifier = Modifier.bringIntoViewRequester(requester))
                }
            },
            actions = {
                when {
                    answered || phase.step.isUngraded() -> PrimaryActionButton(
                        text = stringResource(R.string.lesson_continue),
                        onClick = { onAction(LessonAction.OnContinueClick) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    phase.step.needsCheck() -> PrimaryActionButton(
                        text = stringResource(R.string.lesson_check),
                        onClick = { onAction(LessonAction.OnCheckClick) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = phase.canCheck
                    )
                }
            }
        )
    }
}

private fun StepUi.isUngraded() = this is StepUi.Explain || this is StepUi.Listen || this is StepUi.Shadow

private fun StepUi.needsCheck() = this is StepUi.Typed || this is StepUi.Arrange

@Composable
private fun ResumeContent(title: LocalizedTextUi?, phase: LessonPhase.AskResume, onAction: (LessonAction) -> Unit) {
    Column {
        CloseBar(onAction)
        StepFrame(
            body = {
                title?.let {
                    Text(
                        text = it.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = stringResource(R.string.lesson_resume_title),
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = stringResource(R.string.lesson_resume_body, phase.step, phase.count),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            actions = {
                PrimaryActionButton(
                    text = stringResource(R.string.lesson_resume_continue),
                    onClick = { onAction(LessonAction.OnResumeClick) },
                    modifier = Modifier.fillMaxWidth()
                )
                SecondaryActionButton(
                    text = stringResource(R.string.lesson_resume_restart),
                    onClick = { onAction(LessonAction.OnRestartClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

@Composable
private fun ExitDialog(onAction: (LessonAction) -> Unit) {
    AlertDialog(
        onDismissRequest = { onAction(LessonAction.OnExitDismiss) },
        title = { Text(stringResource(R.string.lesson_exit_title)) },
        text = { Text(stringResource(R.string.lesson_exit_body)) },
        confirmButton = {
            TextActionButton(text = stringResource(R.string.lesson_exit_dismiss), onClick = {
                onAction(LessonAction.OnExitDismiss)
            })
        },
        dismissButton = {
            TextActionButton(text = stringResource(R.string.lesson_exit_confirm), onClick = {
                onAction(LessonAction.OnExitConfirm)
            })
        }
    )
}
