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
package com.gyugle.gil.feature.curriculum.impl.presentation.unitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.domain.repository.CourseRepository
import com.gyugle.gil.feature.curriculum.impl.presentation.toContentErrorUi
import com.gyugle.gil.feature.curriculum.impl.presentation.toLocalizedTextUi
import com.gyugle.gil.feature.progress.api.ProgressObserver
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * A unit's lessons, marked done as the learner finishes them. Lessons are never locked, but a
 * lesson that comes after one not done yet asks first, once per visit: the course builds each
 * lesson on the words of the ones before.
 */
@HiltViewModel(assistedFactory = UnitDetailViewModel.Factory::class)
class UnitDetailViewModel @AssistedInject constructor(
    // A String, not UnitId: Dagger cannot inject a value class parameter.
    @Assisted unitId: String,
    private val courseRepository: CourseRepository,
    private val course: CourseObserver,
    private val progress: ProgressObserver
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(unitId: String): UnitDetailViewModel
    }

    private val unitId = UnitId(unitId)

    private val _state = MutableStateFlow(UnitDetailState())
    val state: StateFlow<UnitDetailState> = _state.asStateFlow()

    private val eventChannel = Channel<UnitDetailEvent>()
    val events = eventChannel.receiveAsFlow()

    private var loadJob: Job? = null
    private var completed: Set<LessonId> = emptySet()
    private var asked = false

    init {
        load()
    }

    fun onAction(action: UnitDetailAction) {
        when (action) {
            UnitDetailAction.OnRetryClick -> load()
            is UnitDetailAction.OnLessonClick -> viewModelScope.launch { open(action.id) }
            UnitDetailAction.OnSkipAheadConfirm -> answerSkipAhead { it.picked }
            UnitDetailAction.OnSkipAheadToEarlier -> answerSkipAhead { it.earlier }
            UnitDetailAction.OnSkipAheadDismiss -> _state.update { it.copy(skipAhead = null) }
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val unit = when (val read = courseRepository.getUnit(unitId)) {
                is Outcome.Success -> read.data

                is Outcome.Failure -> {
                    _state.update { it.copy(isLoading = false, error = read.error.toContentErrorUi()) }
                    return@launch
                }
            }
            progress.observeCompletedLessons()
                .catch { e ->
                    // The lessons still show without the record, none of them marked done.
                    Timber.e(e, "Could not read the finished lessons")
                    emit(emptySet())
                }
                .collect { done ->
                    completed = done
                    _state.update { it.copy(isLoading = false, unit = unit.toUnitDetailUi(done)) }
                }
        }
    }

    private suspend fun open(id: LessonId) {
        val earlier = if (asked || id in completed) {
            null
        } else {
            // Without the course order there is nothing to suggest, and the lesson opens as picked.
            when (val next = course.getNextLesson(completed)) {
                is Outcome.Failure -> {
                    Timber.w("Could not read the course order: %s", next.error)
                    null
                }

                is Outcome.Success -> next.data?.takeIf { it.id != id }
            }
        }
        if (earlier == null) {
            eventChannel.send(UnitDetailEvent.OpenLesson(id))
        } else {
            asked = true
            _state.update { it.copy(skipAhead = SkipAheadUi(id, earlier.id, earlier.title.toLocalizedTextUi())) }
        }
    }

    private fun answerSkipAhead(choice: (SkipAheadUi) -> LessonId) {
        val skipAhead = _state.value.skipAhead ?: return
        _state.update { it.copy(skipAhead = null) }
        viewModelScope.launch { eventChannel.send(UnitDetailEvent.OpenLesson(choice(skipAhead))) }
    }
}
