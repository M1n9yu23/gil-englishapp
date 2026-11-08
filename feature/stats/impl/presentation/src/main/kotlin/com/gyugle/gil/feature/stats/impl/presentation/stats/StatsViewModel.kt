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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.stats.impl.domain.monthOf
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.YearMonth
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * The Stats tab. The figures follow the record as it changes and move to a new day at 04:00 or when
 * the clock or time zone changes, checked once a minute while the screen is shown. The month the
 * calendar shows is kept across process death.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    private val progress: ProgressObserver,
    private val course: CourseObserver,
    private val clock: StudyClock,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val retries = MutableStateFlow(0)

    /** The month the calendar shows as `2026-11`, or null for this month. */
    private val shownMonth = savedStateHandle.getStateFlow<String?>(KEY_SHOWN_MONTH, null)

    val state: StateFlow<StatsState> = combine(today(), retries) { day, _ -> day }
        .flatMapLatest { day -> observeDay(day).onStart { emit(StatsState.Loading) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), StatsState.Loading)

    fun onAction(action: StatsAction) {
        when (action) {
            StatsAction.OnRetryClick -> retries.update { it + 1 }
            StatsAction.OnPreviousMonthClick -> moveMonth(back = true)
            StatsAction.OnNextMonthClick -> moveMonth(back = false)
        }
    }

    private fun moveMonth(back: Boolean) {
        val calendar = (state.value as? StatsState.Ready)?.stats?.calendar ?: return
        if (if (back) !calendar.hasPrevious else !calendar.hasNext) return
        val month = YearMonth.of(calendar.year, calendar.month).plusMonths(if (back) -1 else 1)
        // Back on this month, follow it again, so the calendar moves on when the month ends.
        savedStateHandle[KEY_SHOWN_MONTH] = month.takeIf { it != monthOf(clock.today()) }?.toString()
    }

    /** The study day, again whenever it changes. */
    private fun today(): Flow<StudyDay> = flow {
        while (true) {
            emit(clock.today())
            delay(DAY_CHECK_INTERVAL)
        }
    }.distinctUntilChanged()

    private fun observeDay(day: StudyDay): Flow<StatsState> = flow {
        val stages = when (val read = course.getStageLessons()) {
            is Outcome.Success -> read.data

            is Outcome.Failure -> {
                emit(StatsState.Failed)
                return@flow
            }
        }
        val firstDayOfWeek = firstDayOfWeek()
        val record = combine(
            // Every day on record, for the calendar's months and the totals.
            progress.observeStudyTime(StudyDay(0), day),
            progress.observeCards(),
            progress.observeCompletedLessons(),
            progress.observeStreak(day),
            ::StatsRecord
        )
        emitAll(
            combine(record, shownMonth) { record, month ->
                if (record.isEmpty) {
                    StatsState.Empty(firstLesson = stages.firstNotNullOfOrNull { it.lessons.firstOrNull() })
                } else {
                    // A saved value that does not parse shows this month rather than failing for good.
                    val shown = month?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                    StatsState.Ready(record.toStatsUi(day, firstDayOfWeek, shown, stages))
                }
            }
        )
    }.catch { e ->
        Timber.e(e, "Could not read the study record")
        emit(StatsState.Failed)
    }

    private fun firstDayOfWeek(): DayOfWeek = WeekFields.of(Locale.getDefault()).firstDayOfWeek

    private companion object {
        const val KEY_SHOWN_MONTH = "shownMonth"
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val DAY_CHECK_INTERVAL = 1.minutes
    }
}
