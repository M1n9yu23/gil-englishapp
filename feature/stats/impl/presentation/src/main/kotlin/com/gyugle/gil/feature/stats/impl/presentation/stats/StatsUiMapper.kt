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

import com.gyugle.gil.feature.curriculum.api.StageLessons
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.api.dayOfWeek
import com.gyugle.gil.feature.progress.api.weekOf
import com.gyugle.gil.feature.stats.impl.domain.calendarWeeks
import com.gyugle.gil.feature.stats.impl.domain.countLevels
import com.gyugle.gil.feature.stats.impl.domain.firstDay
import com.gyugle.gil.feature.stats.impl.domain.lastDay
import com.gyugle.gil.feature.stats.impl.domain.monthOf
import com.gyugle.gil.feature.stats.impl.domain.reviewForecast
import com.gyugle.gil.feature.stats.impl.domain.stageProgress
import com.gyugle.gil.feature.stats.impl.domain.totalTime
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * The Stats tab for [today]. The calendar shows [shownMonth], kept between the first month with
 * study time and this month; null shows this month.
 */
internal fun StatsRecord.toStatsUi(
    today: StudyDay,
    firstDayOfWeek: DayOfWeek,
    shownMonth: YearMonth?,
    stages: List<StageLessons>
): StatsUi {
    val thisMonth = monthOf(today)
    val firstMonth = studyTime.firstOrNull()?.let { monthOf(it.day) }?.coerceAtMost(thisMonth) ?: thisMonth
    val month = (shownMonth ?: thisMonth).coerceIn(firstMonth, thisMonth)
    val levels = countLevels(cards)
    return StatsUi(
        streakDays = streak,
        weekMinutes = totalTime(studyTime, weekOf(today, firstDayOfWeek).first(), today).inWholeMinutes.toInt(),
        monthMinutes = totalTime(studyTime, thisMonth.firstDay(), today).inWholeMinutes.toInt(),
        calendar = calendarUi(today, month, firstDayOfWeek, firstMonth, thisMonth),
        forecast = reviewForecast(cards, today).mapIndexed { ahead, count ->
            ForecastDayUi(dayOfWeek(today + ahead), isToday = ahead == 0, count = count)
        },
        levels = LevelsUi(learning = levels.learning, familiar = levels.familiar, lasting = levels.lasting),
        stages = stageProgress(stages, completed).map {
            StageUi(it.stage.title.toLocalizedTextUi(), completed = it.completed, total = it.total)
        }
    )
}

private fun StatsRecord.calendarUi(
    today: StudyDay,
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    firstMonth: YearMonth,
    thisMonth: YearMonth
): CalendarUi {
    val first = month.firstDay()
    val studied = studyTime
        .filter { it.isStudyDay && it.day in first..month.lastDay() }
        .mapTo(mutableSetOf()) { it.day }
    return CalendarUi(
        year = month.year,
        month = month.monthValue,
        weekdays = List(DayOfWeek.entries.size) { firstDayOfWeek.plus(it.toLong()) },
        weeks = calendarWeeks(month, firstDayOfWeek).map { week ->
            week.map { day ->
                day?.let {
                    CalendarDayUi(
                        dayOfMonth = first.daysUntil(it).toInt() + 1,
                        studied = it in studied,
                        isToday = it == today,
                        ahead = it > today
                    )
                }
            }
        },
        studiedDays = studied.size,
        hasPrevious = month > firstMonth,
        hasNext = month < thisMonth
    )
}
