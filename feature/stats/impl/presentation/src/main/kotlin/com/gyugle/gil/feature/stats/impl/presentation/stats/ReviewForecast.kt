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
package com.gyugle.gil.feature.stats.impl.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.ZeroCornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.designsystem.theme.tabularFigures
import com.gyugle.gil.feature.stats.impl.presentation.R
import java.time.format.TextStyle

private object ReviewForecastTokens {
    val BarWidth = 16.dp
    val MaxBarHeight = 72.dp

    /** A day with nothing due keeps a line, so the row of days stays readable. */
    val EmptyBarHeight = 2.dp

    /** One review next to hundreds still shows as a column. */
    val MinBarHeight = 4.dp
}

/**
 * How many reviews fall due on each of the next seven days of [stats], today first, as columns
 * over the weekday. Each column carries its count, so the heights only summarise the numbers.
 */
@Composable
internal fun ReviewForecast(stats: StatsUi, modifier: Modifier = Modifier) {
    val days = stats.forecast
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val today = stringResource(R.string.stats_forecast_today)
    val description = days.map { day ->
        val name = if (day.isToday) today else day.dayOfWeek.getDisplayName(TextStyle.FULL, locale)
        stringResource(R.string.stats_forecast_day, name, day.count)
    }.joinToString(", ")
    val most = days.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)) {
        SectionHeading(title = stringResource(R.string.stats_forecast_title))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = description },
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)
                ) {
                    Text(
                        text = day.count.toString(),
                        style = MaterialTheme.typography.labelMedium.tabularFigures(),
                        color = if (day.count > 0) colors.onSurface else colors.onSurfaceVariant
                    )
                    val bar = Modifier.width(ReviewForecastTokens.BarWidth)
                    if (day.count > 0) {
                        Box(
                            bar
                                .height(
                                    (ReviewForecastTokens.MaxBarHeight * day.count / most)
                                        .coerceAtLeast(ReviewForecastTokens.MinBarHeight)
                                )
                                .background(
                                    colors.primary,
                                    MaterialTheme.shapes.small.copy(
                                        bottomStart = ZeroCornerSize,
                                        bottomEnd = ZeroCornerSize
                                    )
                                )
                        )
                    } else {
                        Box(bar.height(ReviewForecastTokens.EmptyBarHeight).background(colors.outlineVariant))
                    }
                    Text(
                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (day.isToday) colors.onSurface else colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
