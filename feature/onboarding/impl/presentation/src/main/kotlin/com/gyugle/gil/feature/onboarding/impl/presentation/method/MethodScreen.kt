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
package com.gyugle.gil.feature.onboarding.impl.presentation.method

import androidx.annotation.StringRes
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.onboarding.impl.presentation.OnboardingSteps
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingPrimaryButton
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingStepLayout

/**
 * The pages on how studying works here, in the order the first run shows them. As many as
 * [com.gyugle.gil.feature.onboarding.api.OnboardingMethodRoute.PAGE_COUNT]; a test checks it.
 */
internal enum class MethodPage(@param:StringRes val title: Int, @param:StringRes val body: Int) {
    Daily(R.string.onboarding_method_daily_title, R.string.onboarding_method_daily_body),
    Review(R.string.onboarding_method_review_title, R.string.onboarding_method_review_body),
    Sound(R.string.onboarding_method_sound_title, R.string.onboarding_method_sound_body),
    Mistakes(R.string.onboarding_method_mistakes_title, R.string.onboarding_method_mistakes_body)
}

/** One method page. Nothing to load or save, so it has no ViewModel. */
@Composable
fun MethodScreen(page: Int, onNext: () -> Unit, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val content = MethodPage.entries[page]
    OnboardingStepLayout(
        title = stringResource(content.title),
        body = stringResource(content.body),
        modifier = modifier,
        progress = OnboardingSteps.method(page),
        onBack = onBack
    ) {
        OnboardingPrimaryButton(text = stringResource(R.string.onboarding_next), onClick = onNext)
    }
}

@PreviewLightDark
@Composable
private fun MethodScreenPreview() {
    GilTheme {
        Surface {
            MethodScreen(page = 0, onNext = {}, onBack = {})
        }
    }
}
