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
package com.gyugle.gil.feature.onboarding.impl.presentation.welcome

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingPrimaryButton
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingStepLayout

@Composable
fun WelcomeRoot(onStart: () -> Unit, viewModel: WelcomeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WelcomeScreen(state = state, onAction = viewModel::onAction, onStart = onStart)
}

@Composable
fun WelcomeScreen(
    state: WelcomeState,
    onAction: (WelcomeAction) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    OnboardingStepLayout(
        title = stringResource(R.string.onboarding_welcome_title),
        body = stringResource(R.string.onboarding_welcome_body),
        modifier = modifier,
        showMark = true,
        error = state.error
    ) {
        OnboardingPrimaryButton(
            text = stringResource(R.string.onboarding_welcome_start),
            onClick = onStart,
            enabled = !state.isFinishing
        )
        TextActionButton(
            text = stringResource(R.string.onboarding_welcome_skip),
            onClick = { onAction(WelcomeAction.OnSkipClick) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isFinishing
        )
    }
}

@PreviewLightDark
@Composable
private fun WelcomeScreenPreview() {
    GilTheme {
        Surface {
            WelcomeScreen(state = WelcomeState(), onAction = {}, onStart = {})
        }
    }
}
