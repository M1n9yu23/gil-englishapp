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
package com.gyugle.gil.feature.settings.impl.presentation.licenses

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@Immutable
sealed interface LicensesState {
    data object Loading : LicensesState

    data object Failed : LicensesState

    data class Loaded(val libraries: List<LibraryUi>) : LicensesState
}

/** Serves both the list and one library's page; the page finds its entry in the same list. */
@HiltViewModel
class LicensesViewModel @Inject constructor(private val licenses: OpenSourceLicenses) : ViewModel() {
    private val _state = MutableStateFlow<LicensesState>(LicensesState.Loading)
    val state: StateFlow<LicensesState> = _state.asStateFlow()

    init {
        load()
    }

    fun onRetryClick() = load()

    private fun load() {
        _state.value = LicensesState.Loading
        viewModelScope.launch {
            val libraries = licenses.load()
            _state.value = if (libraries.isEmpty()) LicensesState.Failed else LicensesState.Loaded(libraries)
        }
    }
}
