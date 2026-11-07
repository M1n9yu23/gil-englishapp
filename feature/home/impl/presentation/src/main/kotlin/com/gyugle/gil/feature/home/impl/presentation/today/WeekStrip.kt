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
package com.gyugle.gil.feature.home.impl.presentation.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.home.impl.presentation.R
import java.time.format.TextStyle

private object WeekStripTokens {
    val DaySize = 32.dp
    val MarkSize = 18.dp
    val TodayBorder = 2.dp
    val DayBorder = 1.dp
}

/**
 * The streak and the seven days of this week of [today]. A study day is filled with a check, today has a
 * stronger ring, and days to come are faint. Read as one sentence: how many days this week.
 */
@Composable
internal fun WeekStrip(today: TodayUi, modifier: Modifier = Modifier) {
    val streak = today.streak
    val week = today.week
    val colors = MaterialTheme.colorScheme
    val studied = week.count { it.studied }
    val description = pluralStringResource(R.plurals.home_week_description, studied, studied)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.home_week_title),
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium
            )
            if (streak > 0) {
                Text(
                    text = stringResource(R.string.home_streak, streak),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
            }
        }
        val locale = LocalConfiguration.current.locales[0]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            week.forEach { day ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)
                ) {
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day.isToday) colors.onSurface else colors.onSurfaceVariant
                    )
                    DayMark(day)
                }
            }
        }
    }
}

@Composable
private fun DayMark(day: DayUi) {
    val colors = MaterialTheme.colorScheme
    val modifier = Modifier.size(WeekStripTokens.DaySize)
    when {
        day.studied -> Box(
            modifier = modifier.background(colors.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = GilIcons.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(WeekStripTokens.MarkSize)
            )
        }

        day.isToday -> Box(modifier.border(WeekStripTokens.TodayBorder, colors.primary, CircleShape))

        else -> Box(
            modifier.border(
                WeekStripTokens.DayBorder,
                if (day.isFuture) colors.outlineVariant else colors.outline,
                CircleShape
            )
        )
    }
}
