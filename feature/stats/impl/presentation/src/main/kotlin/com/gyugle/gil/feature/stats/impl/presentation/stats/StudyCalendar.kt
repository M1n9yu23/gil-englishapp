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

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.designsystem.theme.tabularFigures
import com.gyugle.gil.feature.stats.impl.presentation.R
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private object StudyCalendarTokens {
    val DaySize = 36.dp
    val DayGap = 2.dp
    val TodayBorder = 2.dp
    val TodayInset = 3.dp

    /** Pulls the month buttons out so their icons line up with the screen margin. */
    val ButtonInset = 12.dp
}

/**
 * The study days of one month, filled like the week on the Today tab, with buttons to the months
 * before and after. Read as one sentence: how many days the learner studied that month.
 */
@Composable
internal fun StudyCalendar(calendar: CalendarUi, onPrevious: () -> Unit, onNext: () -> Unit) {
    val spacing = GilTheme.spacing
    val colors = MaterialTheme.colorScheme
    val locale = LocalConfiguration.current.locales[0]
    val title = remember(calendar.year, calendar.month, locale) {
        val pattern = DateFormat.getBestDateTimePattern(locale, "yMMMM")
        DateTimeFormatter.ofPattern(pattern, locale).format(YearMonth.of(calendar.year, calendar.month))
    }
    val description =
        pluralStringResource(R.plurals.stats_calendar_description, calendar.studiedDays, title, calendar.studiedDays)
    Column(verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)) {
        Row(
            modifier = Modifier.padding(
                start = spacing.screenMargin,
                end = spacing.screenMargin - StudyCalendarTokens.ButtonInset
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium
            )
            IconButton(onClick = onPrevious, enabled = calendar.hasPrevious) {
                Icon(painter = GilIcons.ChevronLeft, contentDescription = stringResource(R.string.stats_previous_month))
            }
            IconButton(onClick = onNext, enabled = calendar.hasNext) {
                Icon(painter = GilIcons.ChevronRight, contentDescription = stringResource(R.string.stats_next_month))
            }
        }
        Column(
            modifier = Modifier
                .padding(horizontal = spacing.screenMargin)
                .clearAndSetSemantics { contentDescription = description },
            verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                calendar.weekdays.forEach { day ->
                    Text(
                        text = day.getDisplayName(TextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            calendar.weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (day != null) DayCell(day)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDayUi) {
    val colors = MaterialTheme.colorScheme
    // As wide as the cell allows up to DaySize, so neighbours never touch when the text is large.
    val shape = Modifier
        .padding(horizontal = StudyCalendarTokens.DayGap)
        .widthIn(max = StudyCalendarTokens.DaySize)
        .fillMaxWidth()
        .aspectRatio(1f)
    val modifier = when {
        // Today keeps its ring once studied, drawn inside the fill in paper colour.
        day.studied && day.isToday ->
            shape
                .background(colors.primary, CircleShape)
                .drawBehind {
                    val stroke = StudyCalendarTokens.TodayBorder.toPx()
                    drawCircle(
                        color = colors.onPrimary,
                        radius = size.minDimension / 2 - StudyCalendarTokens.TodayInset.toPx() - stroke / 2,
                        style = Stroke(width = stroke)
                    )
                }

        day.studied -> shape.background(colors.primary, CircleShape)

        day.isToday -> shape.border(StudyCalendarTokens.TodayBorder, colors.primary, CircleShape)

        else -> shape
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge.tabularFigures(),
            color = when {
                day.studied -> colors.onPrimary
                day.ahead -> colors.onSurfaceVariant
                else -> colors.onSurface
            }
        )
    }
}
