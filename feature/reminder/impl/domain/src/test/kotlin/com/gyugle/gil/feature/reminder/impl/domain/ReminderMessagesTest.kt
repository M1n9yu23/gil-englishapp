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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.reminder.impl.domain.model.ReminderMessage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.jupiter.api.Test

class ReminderMessagesTest {

    @Test
    fun reminderMessageFor_goalMet_isNull() {
        assertThat(reminderMessageFor(DailyGoal.TWENTY, studiedToday = 20.minutes, dueReviews = 12)).isNull()
    }

    @Test
    fun reminderMessageFor_oneSecondShortOfGoal_stillReminds() {
        assertThat(reminderMessageFor(DailyGoal.TWENTY, studiedToday = 20.minutes - 1.seconds, dueReviews = 0))
            .isEqualTo(ReminderMessage.NextLesson)
    }

    @Test
    fun reminderMessageFor_noReviewsDue_pointsToNextLesson() {
        assertThat(reminderMessageFor(DailyGoal.TEN, studiedToday = Duration.ZERO, dueReviews = 0))
            .isEqualTo(ReminderMessage.NextLesson)
    }

    @Test
    fun reminderMessageFor_twelveReviews_aboutSixMinutes() {
        assertThat(reminderMessageFor(DailyGoal.TEN, studiedToday = Duration.ZERO, dueReviews = 12))
            .isEqualTo(ReminderMessage.Reviews(count = 12, minutes = 6))
    }

    @Test
    fun reminderMessageFor_oneReview_atLeastOneMinute() {
        assertThat(reminderMessageFor(DailyGoal.TEN, studiedToday = Duration.ZERO, dueReviews = 1))
            .isEqualTo(ReminderMessage.Reviews(count = 1, minutes = 1))
    }

    @Test
    fun reminderMessageFor_oddReviews_roundsMinutesUp() {
        assertThat(reminderMessageFor(DailyGoal.TEN, studiedToday = Duration.ZERO, dueReviews = 3))
            .isEqualTo(ReminderMessage.Reviews(count = 3, minutes = 2))
    }
}
