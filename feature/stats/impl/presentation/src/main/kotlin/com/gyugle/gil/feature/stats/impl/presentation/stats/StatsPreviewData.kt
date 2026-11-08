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

import com.gyugle.gil.feature.curriculum.api.LessonId
import java.time.DayOfWeek

/** States for previews and screenshot tests: a learner two weeks into November 2026. */
internal object StatsPreviewData {
    private const val TODAY = 18
    private val studied = setOf(2, 3, 4, 6, 9, 10, 11, 12, 13, 16, 17)

    /** November 2026 from Monday: the 1st is a Sunday. */
    private val november = CalendarUi(
        year = 2026,
        month = 11,
        weekdays = List(7) { DayOfWeek.MONDAY.plus(it.toLong()) },
        weeks = (List(6) { null } + (1..30).toList() + List(6) { null }).chunked(7).map { week ->
            week.map { date ->
                date?.let { CalendarDayUi(it, studied = it in studied, isToday = it == TODAY, ahead = it > TODAY) }
            }
        },
        studiedDays = studied.size,
        hasPrevious = true,
        hasNext = false
    )

    private val stats = StatsUi(
        streakDays = 9,
        weekMinutes = 48,
        monthMinutes = 245,
        calendar = november,
        forecast = listOf(14, 3, 0, 6, 9, 1, 4).mapIndexed { ahead, count ->
            ForecastDayUi(DayOfWeek.WEDNESDAY.plus(ahead.toLong()), isToday = ahead == 0, count = count)
        },
        levels = LevelsUi(learning = 46, familiar = 52, lasting = 22),
        stages = listOf(
            StageUi(LocalizedTextUi("소리와 글자", "Sounds and letters"), completed = 12, total = 12),
            StageUi(LocalizedTextUi("알파벳과 읽기", "The alphabet and reading"), completed = 30, total = 30),
            StageUi(LocalizedTextUi("첫걸음", "First steps"), completed = 9, total = 23)
        )
    )

    val ready = StatsState.Ready(stats)

    /** One lesson and no reviews yet: nothing due for days, every item still being learned. */
    val firstWeek = StatsState.Ready(
        stats.copy(
            streakDays = 1,
            weekMinutes = 8,
            monthMinutes = 8,
            calendar = november.copy(
                weeks = november.weeks.map { week ->
                    week.map { day -> day?.copy(studied = day.dayOfMonth == TODAY) }
                },
                studiedDays = 1,
                hasPrevious = false
            ),
            forecast = stats.forecast.mapIndexed { ahead, day -> day.copy(count = if (ahead == 1) 6 else 0) },
            levels = LevelsUi(learning = 6, familiar = 0, lasting = 0),
            stages = listOf(StageUi(LocalizedTextUi("소리와 글자", "Sounds and letters"), completed = 1, total = 12))
        )
    )

    val empty = StatsState.Empty(firstLesson = LessonId("lesson.s0-01.1"))
}
