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
package com.gyugle.gil.feature.reminder.impl.domain

import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.reminder.impl.domain.model.ReminderMessage
import com.gyugle.gil.feature.review.api.REVIEW_TIME_PER_CARD
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The reminder to show, or null when the learner has already met today's [goal]: a reminder then
 * would only interrupt.
 *
 * @param studiedToday study time so far on the current study day.
 * @param dueReviews cards due today or earlier.
 */
fun reminderMessageFor(goal: DailyGoal, studiedToday: Duration, dueReviews: Int): ReminderMessage? {
    if (goal.isMetBy(studiedToday)) return null
    if (dueReviews <= 0) return ReminderMessage.NextLesson
    return ReminderMessage.Reviews(count = dueReviews, minutes = minutesFor(dueReviews))
}

/** Rounded up and at least one, so a few cards never read as "about 0 minutes". */
private fun minutesFor(reviews: Int): Int {
    val total = REVIEW_TIME_PER_CARD * reviews
    val whole = total.inWholeMinutes.toInt()
    return if (total > whole.minutes) whole + 1 else whole.coerceAtLeast(1)
}
