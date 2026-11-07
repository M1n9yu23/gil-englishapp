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

import androidx.compose.runtime.Immutable
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.home.impl.domain.model.NextStep
import java.time.DayOfWeek

/**
 * Today's plan as the screen shows it. [next] is what the one main button opens; [firstLesson]
 * names it as the first lesson.
 */
@Immutable
data class TodayUi(
    val message: TodayMessage?,
    val studiedMinutes: Int,
    val goalMinutes: Int,
    val blocks: List<BlockUi>,
    val next: NextStep?,
    val firstLesson: Boolean,
    val streak: Int,
    val week: List<DayUi>
)

/** One line about the day, when there is something to say. */
enum class TodayMessage { FIRST_DAY, GOAL_MET, REVIEW_HEAVY }

@Immutable
sealed interface BlockUi {
    /** [count] cards due, about [minutes] long. [more] is the rest after the lesson. */
    data class Review(val count: Int, val minutes: Int, val more: Boolean) : BlockUi

    data object ReviewDone : BlockUi

    /** [enoughNewWords] when the lesson would bring more new words than today has room for. */
    data class Lesson(
        val id: LessonId,
        val title: LocalizedTextUi,
        val unitTitle: LocalizedTextUi,
        val minutes: Int,
        val enoughNewWords: Boolean
    ) : BlockUi

    data object CourseDone : BlockUi
}

/** A day of this week: whether it is a study day, today, or still to come. */
@Immutable
data class DayUi(val dayOfWeek: DayOfWeek, val studied: Boolean, val isToday: Boolean, val isFuture: Boolean)
