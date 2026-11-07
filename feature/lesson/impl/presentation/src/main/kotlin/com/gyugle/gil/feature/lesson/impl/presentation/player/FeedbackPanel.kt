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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.lesson.impl.presentation.R
import com.gyugle.gil.feature.lesson.impl.presentation.player.FeedbackUi
import com.gyugle.gil.feature.lesson.impl.presentation.player.GradeUi

/**
 * The result of checking: a short verdict, the right answer when it was not exact, the reason, the
 * translation, and a note when the step comes back. Announced as it appears.
 */
@Composable
internal fun FeedbackPanel(feedback: FeedbackUi, modifier: Modifier = Modifier) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)
    ) {
        Text(
            text = stringResource(
                when (feedback.grade) {
                    GradeUi.CORRECT -> R.string.lesson_feedback_correct
                    GradeUi.CLOSE -> R.string.lesson_feedback_close
                    GradeUi.WRONG -> R.string.lesson_feedback_wrong
                }
            ),
            style = MaterialTheme.typography.titleMedium,
            color = if (feedback.grade == GradeUi.WRONG) colors.error else colors.primary
        )
        feedback.answer?.let { EnglishText(text = it, color = colors.onSurface) }
        if (feedback.typed != null) TypedWords(feedback)
        feedback.why?.let { KoreanProse(text = it, color = colors.onSurfaceVariant) }
        feedback.translation?.let { KoreanProse(text = it, color = colors.onSurfaceVariant) }
        if (feedback.comesBack) {
            Text(
                text = stringResource(R.string.lesson_feedback_comes_back),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }
    }
}

/** What the learner wrote, a wrong word underlined and announced as such. */
@Composable
private fun TypedWords(feedback: FeedbackUi) {
    val colors = MaterialTheme.colorScheme
    val wrong = stringResource(R.string.lesson_feedback_word_wrong)
    Column(verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap / 2)) {
        Text(
            text = stringResource(R.string.lesson_feedback_you_wrote),
            style = MaterialTheme.typography.labelMedium,
            color = colors.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)) {
            feedback.typed.orEmpty().forEach { (text, matched) ->
                EnglishText(
                    text = text,
                    color = if (matched) colors.onSurface else colors.error,
                    textDecoration = if (matched) null else TextDecoration.Underline,
                    modifier = if (matched) Modifier else Modifier.semantics { stateDescription = wrong }
                )
            }
        }
    }
}
