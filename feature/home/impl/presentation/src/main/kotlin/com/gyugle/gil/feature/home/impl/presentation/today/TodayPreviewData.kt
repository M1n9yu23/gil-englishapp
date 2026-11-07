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

import com.gyugle.gil.feature.curriculum.api.LessonId
import java.time.DayOfWeek

/** States for previews and screenshot tests. */
internal object TodayPreviewData {
    private val lessonId = LessonId("lesson.s1-17.1")

    private val lesson = BlockUi.Lesson(
        id = lessonId,
        title = LocalizedTextUi("나는 학생이에요", "I am a student"),
        unitTitle = LocalizedTextUi("나와 너", "You and me"),
        minutes = 12,
        enoughNewWords = false
    )

    private val firstLesson = lesson.copy(
        id = LessonId("lesson.s0-01.1"),
        title = LocalizedTextUi("영어 소리와 만나기", "Meet the sounds of English"),
        unitTitle = LocalizedTextUi("길 위에 서기", "Getting started"),
        minutes = 8
    )

    private val twelveReviews = BlockUi.Review(count = 12, minutes = 6, more = false)

    private val tenReviews = BlockUi.Review(count = 10, minutes = 5, more = false)

    private val nextLesson = lesson.copy(title = LocalizedTextUi("이것은 책이에요", "This is a book"))

    /** Monday to Sunday with today on Thursday, studied Monday to Wednesday. */
    private fun week(studiedToday: Boolean = false) = DayOfWeek.entries.mapIndexed { index, day ->
        DayUi(
            dayOfWeek = day,
            studied = index < 3 || (index == 3 && studiedToday),
            isToday = index == 3,
            muted = index > 3
        )
    }

    val firstDay = TodayState.Ready(
        TodayUi(
            message = TodayMessage.FIRST_DAY,
            studiedMinutes = 0,
            goalMinutes = 20,
            blocks = listOf(firstLesson),
            next = firstLesson,
            firstLesson = true,
            streak = 0,
            week = DayOfWeek.entries.mapIndexed { index, day ->
                DayUi(day, studied = false, isToday = index == 3, muted = index != 3)
            }
        )
    )

    val reviewsDue = TodayState.Ready(
        TodayUi(
            message = null,
            studiedMinutes = 4,
            goalMinutes = 20,
            blocks = listOf(twelveReviews, lesson),
            next = twelveReviews,
            firstLesson = false,
            streak = 3,
            week = week()
        )
    )

    val reviewHeavy = TodayState.Ready(
        TodayUi(
            message = TodayMessage.REVIEW_HEAVY,
            studiedMinutes = 0,
            goalMinutes = 10,
            blocks = listOf(
                tenReviews,
                lesson,
                BlockUi.Review(count = 23, minutes = 12, more = true)
            ),
            next = tenReviews,
            firstLesson = false,
            streak = 3,
            week = week()
        )
    )

    val goalMet = TodayState.Ready(
        TodayUi(
            message = TodayMessage.GOAL_MET,
            studiedMinutes = 23,
            goalMinutes = 20,
            blocks = listOf(BlockUi.ReviewDone, nextLesson),
            next = nextLesson,
            firstLesson = false,
            streak = 4,
            week = week(studiedToday = true)
        )
    )

    val courseDone = TodayState.Ready(
        TodayUi(
            message = null,
            studiedMinutes = 2,
            goalMinutes = 30,
            blocks = listOf(BlockUi.ReviewDone, BlockUi.CourseDone),
            next = null,
            firstLesson = false,
            streak = 12,
            week = week()
        )
    )
}
