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

import com.gyugle.gil.feature.home.impl.domain.dayOfWeek
import com.gyugle.gil.feature.home.impl.domain.model.NextStep
import com.gyugle.gil.feature.home.impl.domain.model.TodayPlan
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.StudyDay

/**
 * The plan for [today] as the screen shows it, with the study time of [week], whose days are in
 * order, and the [streak].
 */
internal fun TodayPlan.toTodayUi(
    today: StudyDay,
    goalMinutes: Int,
    week: List<StudyDay>,
    studyTime: List<DailyStudyTime>,
    streak: Int
): TodayUi {
    val byDay = studyTime.associateBy { it.day }
    val blocks = blocks()
    return TodayUi(
        message = when {
            firstDay -> TodayMessage.FIRST_DAY
            goalMet -> TodayMessage.GOAL_MET
            reviewHeavy -> TodayMessage.REVIEW_HEAVY
            else -> null
        },
        studiedMinutes = byDay[today]?.duration?.inWholeMinutes?.toInt() ?: 0,
        goalMinutes = goalMinutes,
        blocks = blocks,
        next = when (val step = next) {
            is NextStep.Review -> blocks.firstOrNull { it is BlockUi.Review && it.count == step.limit }
            is NextStep.Lesson -> blocks.firstOrNull { it is BlockUi.Lesson }
            null -> null
        },
        firstLesson = firstDay,
        streak = streak,
        week = week.map { day ->
            DayUi(
                dayOfWeek = dayOfWeek(day),
                studied = byDay[day]?.isStudyDay == true,
                isToday = day == today,
                muted = day > today || (firstDay && day < today)
            )
        }
    )
}

private fun TodayPlan.blocks(): List<BlockUi> = buildList {
    val first = review
    when {
        first != null -> add(BlockUi.Review(first.count, first.minutes, more = false))
        reviewDoneToday -> add(BlockUi.ReviewDone)
    }
    val next = lesson
    if (next == null) {
        add(BlockUi.CourseDone)
    } else {
        add(
            BlockUi.Lesson(
                id = next.lesson.id,
                title = next.lesson.title.toLocalizedTextUi(),
                unitTitle = next.lesson.unitTitle.toLocalizedTextUi(),
                // About a minute a step.
                minutes = next.lesson.stepCount,
                // On a review-heavy day the message above already says new words wait.
                enoughNewWords = !next.recommended && !reviewHeavy
            )
        )
    }
    moreReview?.let { add(BlockUi.Review(it.count, it.minutes, more = true)) }
}
