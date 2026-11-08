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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.onboarding.impl.presentation.R
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingPrimaryButton
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingProgress
import com.gyugle.gil.feature.onboarding.impl.presentation.component.OnboardingStepLayout

/**
 * The method pages again, outside the first run: the progress counts only these pages, back
 * goes to the page before, and the last page closes. The page is plain UI state, saved across
 * rotation and process death, so there is no ViewModel.
 */
@Composable
fun StudyMethodScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val pages = MethodPage.entries
    BackHandler(enabled = page > 0) { page-- }
    val fade = GilTheme.motion.fade
    AnimatedContent(
        targetState = page,
        transitionSpec = { fadeIn(tween(fade)) togetherWith fadeOut(tween(fade)) },
        label = "method"
    ) { shown ->
        val content = pages[shown]
        val last = shown == pages.lastIndex
        OnboardingStepLayout(
            title = stringResource(content.title),
            body = stringResource(content.body),
            modifier = modifier,
            progress = OnboardingProgress(step = shown + 1, count = pages.size),
            onBack = { if (shown > 0) page = shown - 1 else onClose() }
        ) {
            OnboardingPrimaryButton(
                text = stringResource(if (last) R.string.onboarding_method_done else R.string.onboarding_next),
                onClick = { if (last) onClose() else page = shown + 1 }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun StudyMethodScreenPreview() {
    GilTheme {
        Surface {
            StudyMethodScreen(onClose = {})
        }
    }
}
