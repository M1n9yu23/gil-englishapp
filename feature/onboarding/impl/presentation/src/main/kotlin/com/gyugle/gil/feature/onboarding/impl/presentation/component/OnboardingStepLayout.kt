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
package com.gyugle.gil.feature.onboarding.impl.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.R as CoreR
import com.gyugle.gil.core.designsystem.component.GilProgressBar
import com.gyugle.gil.core.designsystem.component.PathMark
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.onboarding.impl.presentation.R

private object OnboardingStepTokens {
    /** The bar row is one icon button tall, with or without the back arrow. */
    val BarHeight = 48.dp

    // 4dp outside a 48dp icon button puts the arrow on the 16dp keyline, as in the top bar.
    val IconEdgePadding = 4.dp

    // The same gap the empty states leave between the motif and their title.
    val MarkToTitle = 24.dp
}

/** Where a step sits in the first run, after the welcome screen. */
internal data class OnboardingProgress(val step: Int, val count: Int)

/**
 * The frame of every first-run step: progress at the top, the path motif when [showMark] (the
 * welcome screen), the title and text, the step's own
 * [stepContent], and the [actions] kept at the bottom. [stepContent] spans the full width, so list
 * rows reach the edges; anything else in it keeps the screen margin itself. Everything above the actions scrolls, so a
 * large font never pushes the buttons off the screen or cuts the text.
 */
@Composable
internal fun OnboardingStepLayout(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    progress: OnboardingProgress? = null,
    onBack: (() -> Unit)? = null,
    showMark: Boolean = false,
    error: UiText? = null,
    stepContent: @Composable ColumnScope.() -> Unit = {},
    actions: @Composable ColumnScope.() -> Unit
) {
    val spacing = GilTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        StepBar(progress, onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = spacing.itemGap, bottom = spacing.sectionGap)
        ) {
            if (showMark) {
                PathMark(modifier = Modifier.padding(horizontal = spacing.screenMargin))
                Spacer(Modifier.height(OnboardingStepTokens.MarkToTitle))
            }
            Text(
                text = title,
                modifier = Modifier
                    .padding(horizontal = spacing.screenMargin)
                    .semantics { heading() },
                style = MaterialTheme.typography.headlineSmall.copy(lineBreak = LineBreak.Heading),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(spacing.itemGap))
            Text(
                text = body,
                modifier = Modifier.padding(horizontal = spacing.screenMargin),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(spacing.sectionGap))
            stepContent()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenMargin)
                .padding(bottom = spacing.screenMargin),
            verticalArrangement = Arrangement.spacedBy(spacing.inlineGap),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (error != null) {
                Text(
                    text = error.asString(),
                    // A failed save appears without focus moving, so screen readers announce it.
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            actions()
        }
    }
}

@Composable
private fun StepBar(progress: OnboardingProgress?, onBack: (() -> Unit)?) {
    val spacing = GilTheme.spacing
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = OnboardingStepTokens.BarHeight)
            .padding(
                start = if (onBack == null) spacing.screenMargin else OnboardingStepTokens.IconEdgePadding,
                end = spacing.screenMargin
            ),
        horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    painter = GilIcons.ArrowBack,
                    contentDescription = stringResource(CoreR.string.common_navigate_back)
                )
            }
        }
        if (progress != null) {
            val description = stringResource(R.string.onboarding_progress_description, progress.step, progress.count)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = description },
                horizontalArrangement = Arrangement.spacedBy(spacing.itemGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GilProgressBar(progress = progress.step.toFloat() / progress.count, modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.onboarding_progress, progress.step, progress.count),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A primary action across the width, the shape every step's main button takes. */
@Composable
internal fun OnboardingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PrimaryActionButton(text = text, onClick = onClick, modifier = modifier.fillMaxWidth(), enabled = enabled)
}

@PreviewLightDark
@Composable
private fun OnboardingStepLayoutPreview() {
    GilTheme {
        Surface {
            OnboardingStepLayout(
                title = "매일 조금씩",
                body = "일주일에 한 번 한 시간보다 매일 10분이 더 오래 남아요.",
                progress = OnboardingProgress(step = 1, count = 7),
                onBack = {}
            ) {
                OnboardingPrimaryButton(text = "다음", onClick = {})
                TextActionButton(text = "건너뛰기", onClick = {})
            }
        }
    }
}
