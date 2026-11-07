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
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.domain.repository.CourseRepository
import com.gyugle.gil.feature.curriculum.impl.presentation.toContentErrorUi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = UnitDetailViewModel.Factory::class)
class UnitDetailViewModel @AssistedInject constructor(
    // A String, not UnitId: Dagger cannot inject a value class parameter.
    @Assisted unitId: String,
    private val courseRepository: CourseRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(unitId: String): UnitDetailViewModel
    }

    private val unitId = UnitId(unitId)

    private val _state = MutableStateFlow(UnitDetailState())
    val state: StateFlow<UnitDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun onAction(action: UnitDetailAction) {
        when (action) {
            UnitDetailAction.OnRetryClick -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            courseRepository.getUnit(unitId)
                .onSuccess { unit -> _state.update { it.copy(isLoading = false, unit = unit.toUnitDetailUi()) } }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.toContentErrorUi()) } }
        }
    }
}
