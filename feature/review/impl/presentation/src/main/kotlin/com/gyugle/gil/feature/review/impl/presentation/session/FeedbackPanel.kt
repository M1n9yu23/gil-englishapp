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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.Reveal
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.review.impl.presentation.R

/**
 * The verdict, the right answer when a typed one was not exact, what the card did not show before,
 * and a note that a missed card comes back. Announced as it appears, and fades in with the answers.
 */
@Composable
internal fun FeedbackPanel(feedback: FeedbackUi, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Reveal(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)
        ) {
            Text(
                text = stringResource(
                    when (feedback.grade) {
                        GradeUi.CORRECT -> R.string.review_feedback_correct
                        GradeUi.CLOSE -> R.string.review_feedback_close
                        GradeUi.WRONG -> R.string.review_feedback_wrong
                    }
                ),
                style = MaterialTheme.typography.titleMedium,
                color = if (feedback.grade == GradeUi.WRONG) colors.error else colors.primary
            )
            feedback.answer?.let { EnglishText(text = it, color = colors.onSurface) }
            feedback.note?.let { KoreanProse(text = it, color = colors.onSurfaceVariant) }
            if (feedback.grade == GradeUi.WRONG) {
                Text(
                    text = stringResource(R.string.review_feedback_comes_back),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}
