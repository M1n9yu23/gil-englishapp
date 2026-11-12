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
package com.gyugle.gil.feature.curriculum.impl.presentation.path

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.impl.domain.repository.CourseRepository
import com.gyugle.gil.feature.curriculum.impl.presentation.toContentErrorUi
import com.gyugle.gil.feature.progress.api.ProgressObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * The course's stages, with the learner's place on them. The place follows the finished lessons,
 * so finishing a lesson or passing a stage test moves it while the tab is open.
 */
@HiltViewModel
class PathViewModel @Inject constructor(
    private val courseRepository: CourseRepository,
    private val progress: ProgressObserver
) : ViewModel() {

    private val _state = MutableStateFlow(PathState())
    val state: StateFlow<PathState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun onAction(action: PathAction) {
        when (action) {
            PathAction.OnRetryClick -> load()
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val stages = when (val read = courseRepository.getStages()) {
                is Outcome.Success -> read.data

                is Outcome.Failure -> {
                    _state.update { it.copy(isLoading = false, error = read.error.toContentErrorUi()) }
                    return@launch
                }
            }
            progress.observeCompletedLessons()
                .catch { e ->
                    // The path still shows without the record, as for a learner who has just started.
                    Timber.e(e, "Could not read the finished lessons")
                    emit(emptySet<LessonId>())
                }
                .collect { completed ->
                    _state.update { it.copy(isLoading = false, stages = stages.toStageUis(completed)) }
                }
        }
    }
}
