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
import androidx.compose.foundation.layout.padding
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

/**
 * What the phone heard in the last recording. A missed word is underlined and muted rather than
 * marked wrong: the recognizer can miss a word the learner said well.
 */
@Composable
internal fun ScoreView(score: ScoreUi) {
    val colors = MaterialTheme.colorScheme
    // Each state is read out as it arrives: the result comes after the recording stops, away from
    // where a screen reader's focus is.
    val announced = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
    when (score) {
        ScoreUi.Checking -> Text(
            text = stringResource(R.string.lesson_score_checking),
            modifier = announced,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )

        // A little apart from the buttons: this block is a result, not another control.
        is ScoreUi.Heard -> Column(
            verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap / 2),
            modifier = announced.padding(top = GilTheme.spacing.inlineGap)
        ) {
            Text(
                text = stringResource(R.string.lesson_score_heard),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant
            )
            val missed = stringResource(R.string.lesson_score_word_missed)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)) {
                score.words.forEach { (text, matched) ->
                    EnglishText(
                        text = text,
                        color = if (matched) colors.onSurface else colors.onSurfaceVariant,
                        textDecoration = if (matched) null else TextDecoration.Underline,
                        modifier = if (matched) Modifier else Modifier.semantics { stateDescription = missed }
                    )
                }
            }
            KoreanProse(
                text = stringResource(
                    if (score.allMatched) R.string.lesson_score_all_heard else R.string.lesson_score_some_missed
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant
            )
        }

        ScoreUi.NothingHeard -> KoreanProse(
            text = stringResource(R.string.lesson_score_nothing_heard),
            modifier = announced,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant
        )
    }
}

/** Why recordings are compared by ear only, shown once ever. */
@Composable
internal fun ScoringNotice(notice: ScoringNoticeUi) {
    KoreanProse(
        text = stringResource(
            when (notice) {
                ScoringNoticeUi.MODEL_NOT_INSTALLED -> R.string.lesson_scoring_model_missing
                ScoringNoticeUi.UNSUPPORTED -> R.string.lesson_scoring_unsupported
            }
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
