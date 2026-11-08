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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.home.impl.domain.introducedOn
import com.gyugle.gil.feature.home.impl.domain.planToday
import com.gyugle.gil.feature.home.impl.domain.reviewedOn
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.DailyStudyTime
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.progress.api.weekOf
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * The Today tab. The plan follows the record as it changes, and moves to a new day at 04:00 or
 * when the clock or time zone changes, checked once a minute while the screen is shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val goals: GoalObserver,
    private val progress: ProgressObserver,
    private val course: CourseObserver,
    private val clock: StudyClock
) : ViewModel() {

    private val retries = MutableStateFlow(0)

    val state: StateFlow<TodayState> = combine(today(), retries) { day, _ -> day }
        .flatMapLatest { day -> observeDay(day).onStart { emit(TodayState.Loading) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), TodayState.Loading)

    fun onAction(action: TodayAction) {
        when (action) {
            TodayAction.OnRetryClick -> retries.update { it + 1 }
        }
    }

    /** The study day, again whenever it changes. */
    private fun today(): Flow<StudyDay> = flow {
        while (true) {
            emit(clock.today())
            delay(DAY_CHECK_INTERVAL)
        }
    }.distinctUntilChanged()

    private fun observeDay(day: StudyDay): Flow<TodayState> {
        val week = weekOf(day, firstDayOfWeek())
        val record = combine(
            progress.observeDueCards(day),
            progress.observeCards(),
            progress.observeCompletedLessons(),
            progress.observeStudyTime(week.first(), week.last()),
            progress.observeStreak(day)
        ) { due, cards, completed, studyTime, streak -> Record(due.size, cards, completed, studyTime, streak) }
        return combine(goals.observeGoal(), record, ::Pair)
            .mapLatest { (goal, record) -> todayState(day, week, goal, record) }
            .catch { e ->
                Timber.e(e, "Could not read the study record")
                emit(TodayState.Failed)
            }
    }

    private suspend fun todayState(day: StudyDay, week: List<StudyDay>, goal: DailyGoal, record: Record): TodayState {
        val next = when (val read = course.getNextLesson(record.completed)) {
            is Outcome.Success -> read.data
            is Outcome.Failure -> return TodayState.Failed
        }
        val studied = record.studyTime.firstOrNull { it.day == day }?.duration ?: Duration.ZERO
        val plan = planToday(
            goal = goal,
            studiedToday = studied,
            dueCount = record.dueCount,
            reviewedToday = reviewedOn(day, record.cards),
            introducedToday = introducedOn(day, record.cards),
            nextLesson = next,
            hasRecord = record.cards.isNotEmpty() || record.completed.isNotEmpty()
        )
        return TodayState.Ready(plan.toTodayUi(day, goal.minutes, week, record.studyTime, record.streak))
    }

    private fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private class Record(
        val dueCount: Int,
        val cards: List<CardState>,
        val completed: Set<LessonId>,
        val studyTime: List<DailyStudyTime>,
        val streak: Int
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val DAY_CHECK_INTERVAL = 1.minutes
    }
}
