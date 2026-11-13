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
package com.gyugle.gil.feature.onboarding.impl.presentation.goal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.RowDivider
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.ObserveAsEvents
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.onboarding.impl.presentation.OnboardingSteps
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingPrimaryButton
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingStepLayout

private object GoalStepTokens {
    val RowMinHeight = 56.dp
}

@Composable
fun GoalStepRoot(onNext: () -> Unit, onBack: () -> Unit, viewModel: GoalStepViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            GoalStepEvent.Saved -> onNext()
        }
    }
    GoalStepScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun GoalStepScreen(
    state: GoalStepState,
    onAction: (GoalStepAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepLayout(
        title = stringResource(R.string.onboarding_goal_title),
        body = stringResource(R.string.onboarding_goal_body),
        modifier = modifier,
        progress = OnboardingSteps.goal,
        onBack = onBack,
        error = state.error,
        stepContent = {
            Column(modifier = Modifier.selectableGroup()) {
                DailyGoal.entries.forEachIndexed { index, goal ->
                    if (index > 0) RowDivider()
                    GoalRow(
                        goal = goal,
                        selected = goal == state.selected,
                        onSelect = { onAction(GoalStepAction.OnGoalSelect(goal)) }
                    )
                }
            }
        }
    ) {
        OnboardingPrimaryButton(
            text = stringResource(R.string.onboarding_next),
            onClick = { onAction(GoalStepAction.OnNextClick) },
            enabled = !state.isSaving
        )
    }
}

@Composable
private fun GoalRow(goal: DailyGoal, selected: Boolean, onSelect: () -> Unit) {
    val spacing = GilTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = GoalStepTokens.RowMinHeight)
            .selectable(selected = selected, onClick = onSelect, role = Role.RadioButton)
            .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.onboarding_goal_minutes, goal.minutes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(goal.detail()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        // The row is the touch target and carries the state; the button only draws it.
        RadioButton(
            selected = selected,
            onClick = null,
            colors = RadioButtonDefaults.colors(unselectedColor = MaterialTheme.colorScheme.outline)
        )
    }
}

private fun DailyGoal.detail(): Int = when (this) {
    DailyGoal.TEN -> R.string.onboarding_goal_10_detail
    DailyGoal.TWENTY -> R.string.onboarding_goal_20_detail
    DailyGoal.THIRTY -> R.string.onboarding_goal_30_detail
    DailyGoal.SIXTY -> R.string.onboarding_goal_60_detail
}

@PreviewLightDark
@Composable
private fun GoalStepScreenPreview() {
    GilTheme {
        Surface {
            GoalStepScreen(state = GoalStepState(), onAction = {}, onBack = {})
        }
    }
}
