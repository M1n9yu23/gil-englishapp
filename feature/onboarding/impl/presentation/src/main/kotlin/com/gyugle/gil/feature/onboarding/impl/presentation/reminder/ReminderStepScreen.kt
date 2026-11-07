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
package com.gyugle.gil.feature.onboarding.impl.presentation.reminder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.core.presentation.permissions.Permission
import com.gyugle.gil.core.presentation.permissions.PermissionState
import com.gyugle.gil.core.presentation.permissions.rememberPermissionController
import com.gyugle.gil.feature.onboarding.impl.presentation.OnboardingSteps
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingPrimaryButton
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingStepLayout
import com.gyugle.gil.feature.reminder.api.ReminderTime

private object ReminderStepTokens {
    /** The screen margin less a text button's 12dp inner padding. */
    val AddButtonStart = 8.dp
}

@Composable
fun ReminderStepRoot(onNext: () -> Unit, onBack: () -> Unit, viewModel: ReminderStepViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val permissions = rememberPermissionController()
    val context = LocalContext.current

    // Below Android 13 nothing is asked, but the app's notifications can still be turned off.
    fun notificationState(): PermissionState {
        val state = permissions.getPermissionState(Permission.NOTIFICATIONS)
        val enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        return if (state == PermissionState.GRANTED && !enabled) PermissionState.PERMANENTLY_DENIED else state
    }
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            ReminderStepEvent.CheckPermission ->
                viewModel.onAction(ReminderStepAction.OnPermissionChecked(notificationState()))

            ReminderStepEvent.RequestPermission -> viewModel.onAction(
                ReminderStepAction.OnPermissionResult(permissions.requestPermission(Permission.NOTIFICATIONS))
            )

            ReminderStepEvent.OpenAppSettings -> permissions.openAppSettings()

            ReminderStepEvent.Done -> onNext()
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(ReminderStepAction.OnResumed(notificationState()))
    }
    ReminderStepScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun ReminderStepScreen(
    state: ReminderStepState,
    onAction: (ReminderStepAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backToTimes = { onAction(ReminderStepAction.OnBackToTimesClick) }
    BackHandler(enabled = state.stage != ReminderStage.Times, onBack = backToTimes)
    when (val stage = state.stage) {
        ReminderStage.Times -> TimesStage(state, onAction, onBack, modifier)

        ReminderStage.Rationale -> PermissionStage(
            title = stringResource(R.string.onboarding_reminder_rationale_title),
            body = stringResource(R.string.onboarding_reminder_rationale_body),
            primaryLabel = stringResource(R.string.onboarding_continue),
            onPrimary = { onAction(ReminderStepAction.OnAllowClick) },
            state = state,
            onAction = onAction,
            onBack = backToTimes,
            modifier = modifier
        )

        is ReminderStage.Denied -> PermissionStage(
            title = stringResource(stage.title()),
            body = stringResource(stage.body()),
            primaryLabel = stringResource(
                if (stage.permanently) R.string.onboarding_reminder_open_settings else R.string.onboarding_next
            ),
            onPrimary = {
                onAction(
                    if (stage.permanently) ReminderStepAction.OnOpenSettingsClick else ReminderStepAction.OnSkipClick
                )
            },
            state = state,
            onAction = onAction,
            onBack = backToTimes,
            modifier = modifier,
            showSkip = stage.permanently
        )
    }
    val editing = state.editing
    if (editing != null && state.stage == ReminderStage.Times) {
        ReminderTimePickerDialog(
            initial = state.times.getOrNull(editing) ?: suggestionFor(state.times),
            onPick = { time -> onAction(ReminderStepAction.OnTimePicked(time)) },
            onDismiss = { onAction(ReminderStepAction.OnPickerDismiss) }
        )
    }
}

private fun ReminderStage.Denied.title(): Int =
    if (permanently) R.string.onboarding_reminder_blocked_title else R.string.onboarding_reminder_denied_title

private fun ReminderStage.Denied.body(): Int =
    if (permanently) R.string.onboarding_reminder_blocked_body else R.string.onboarding_reminder_denied_body

private fun suggestionFor(times: List<ReminderTime>): ReminderTime =
    if (ReminderStepViewModel.SECOND_SUGGESTION in times) {
        ReminderStepViewModel.FIRST_SUGGESTION
    } else {
        ReminderStepViewModel.SECOND_SUGGESTION
    }

@Composable
private fun TimesStage(
    state: ReminderStepState,
    onAction: (ReminderStepAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepLayout(
        title = stringResource(R.string.onboarding_reminder_title),
        body = stringResource(R.string.onboarding_reminder_body),
        modifier = modifier,
        progress = OnboardingSteps.reminder,
        onBack = onBack,
        error = state.error,
        stepContent = {
            Column {
                state.times.forEachIndexed { index, time ->
                    if (index > 0) HorizontalDivider()
                    TimeRow(
                        time = time,
                        onClick = { onAction(ReminderStepAction.OnTimeClick(index)) },
                        onRemove = { onAction(ReminderStepAction.OnRemoveTimeClick(index)) }
                    )
                }
            }
            if (state.canAddTime) {
                TextActionButton(
                    text = stringResource(R.string.onboarding_reminder_add),
                    onClick = { onAction(ReminderStepAction.OnAddTimeClick) },
                    // The button's own inner padding brings its text to the screen margin.
                    modifier = Modifier.padding(
                        start = ReminderStepTokens.AddButtonStart,
                        top = GilTheme.spacing.inlineGap
                    )
                )
            }
        }
    ) {
        OnboardingPrimaryButton(
            text = stringResource(R.string.onboarding_reminder_enable),
            onClick = { onAction(ReminderStepAction.OnEnableClick) },
            enabled = state.times.isNotEmpty() && !state.isSaving
        )
        TextActionButton(
            text = stringResource(R.string.onboarding_reminder_skip),
            onClick = { onAction(ReminderStepAction.OnSkipClick) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving
        )
    }
}

@Composable
private fun TimeRow(time: ReminderTime, onClick: () -> Unit, onRemove: () -> Unit) {
    val text = time.displayText()
    ListRow(
        headline = text,
        onClick = onClick,
        clickLabel = stringResource(R.string.onboarding_reminder_change_time),
        trailing = {
            IconButton(onClick = onRemove) {
                Icon(
                    painter = GilIcons.Close,
                    contentDescription = stringResource(R.string.onboarding_reminder_remove, text)
                )
            }
        }
    )
}

/** The rationale and the refusal: no list, only what happened and what to do next. */
@Composable
private fun PermissionStage(
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    state: ReminderStepState,
    onAction: (ReminderStepAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    showSkip: Boolean = true
) {
    OnboardingStepLayout(
        title = title,
        body = body,
        modifier = modifier,
        progress = OnboardingSteps.reminder,
        onBack = onBack,
        error = state.error
    ) {
        OnboardingPrimaryButton(text = primaryLabel, onClick = onPrimary, enabled = !state.isSaving)
        if (showSkip) {
            TextActionButton(
                text = stringResource(R.string.onboarding_reminder_skip),
                onClick = { onAction(ReminderStepAction.OnSkipClick) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun ReminderStepScreenPreview() {
    GilTheme {
        Surface {
            ReminderStepScreen(
                state = ReminderStepState(times = listOf(ReminderTime(8, 0), ReminderTime(20, 0))),
                onAction = {},
                onBack = {}
            )
        }
    }
}
