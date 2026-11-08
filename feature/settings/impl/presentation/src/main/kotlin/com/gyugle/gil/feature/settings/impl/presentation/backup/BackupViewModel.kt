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
package com.gyugle.gil.feature.settings.impl.presentation.backup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.settings.impl.domain.repository.BackupRepository
import com.gyugle.gil.feature.settings.impl.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The picked file's URI is kept in [SavedStateHandle] while its summary shows, so after process
 * death the summary is read again instead of lost. A restore runs to the end even if the learner
 * leaves; the repository makes sure of that.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val backupRepository: BackupRepository,
    private val clock: StudyClock
) : ViewModel() {

    private val _state = MutableStateFlow<BackupState>(BackupState.Idle)
    val state: StateFlow<BackupState> = _state.asStateFlow()

    private val eventChannel = Channel<BackupEvent>()
    val events: Flow<BackupEvent> = eventChannel.receiveAsFlow()

    init {
        savedStateHandle.get<String>(KEY_FILE)?.let(::read)
    }

    fun onAction(action: BackupAction) {
        when (action) {
            BackupAction.OnExportClick -> if (_state.value ==
                BackupState.Idle
            ) {
                send(BackupEvent.PickExportFile(fileName()))
            }

            is BackupAction.OnExportPicked -> action.uri?.let(::export)

            BackupAction.OnImportClick -> if (_state.value == BackupState.Idle) send(BackupEvent.PickImportFile)

            is BackupAction.OnImportPicked -> action.uri?.let(::read)

            BackupAction.OnRestoreClick -> restore()

            BackupAction.OnDoneClick -> {
                savedStateHandle.remove<String>(KEY_FILE)
                _state.value = BackupState.Idle
            }
        }
    }

    private fun export(uri: String) {
        if (_state.value != BackupState.Idle) return
        _state.value = BackupState.Exporting
        viewModelScope.launch {
            backupRepository.export(uri)
                .onSuccess {
                    _state.value = BackupState.Idle
                    eventChannel.send(BackupEvent.ShowMessage(UiText.StringResource(R.string.settings_exported)))
                }
                .onFailure { error -> _state.value = BackupState.Failed(BackupStep.EXPORT, error.toUiText()) }
        }
    }

    private fun read(uri: String) {
        savedStateHandle[KEY_FILE] = uri
        _state.value = BackupState.Reading
        viewModelScope.launch {
            backupRepository.preview(uri)
                .onSuccess { preview -> _state.value = BackupState.Preview(preview) }
                .onFailure { error ->
                    savedStateHandle.remove<String>(KEY_FILE)
                    _state.value = BackupState.Failed(BackupStep.READ, error.toUiText())
                }
        }
    }

    private fun restore() {
        val preview = (_state.value as? BackupState.Preview)?.preview ?: return
        val uri = savedStateHandle.get<String>(KEY_FILE) ?: return
        _state.value = BackupState.Restoring(preview)
        viewModelScope.launch {
            val result = backupRepository.restore(uri)
            savedStateHandle.remove<String>(KEY_FILE)
            result
                .onSuccess { _state.value = BackupState.Restored }
                .onFailure { error ->
                    _state.value = BackupState.Failed(BackupStep.RESTORE, error.toUiText(), unchanged = true)
                }
        }
    }

    /** `gil-backup-2026-11-08.json`, the day in the phone's time zone. */
    private fun fileName(): String {
        val day = Instant.ofEpochMilli(clock.now().toEpochMilliseconds()).atZone(ZoneId.systemDefault()).toLocalDate()
        return "gil-backup-$day.json"
    }

    private fun send(event: BackupEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private companion object {
        const val KEY_FILE = "file"
    }
}

/** The step names what failed in the title; this says why. */
private fun BackupError.toUiText(): UiText = UiText.StringResource(
    when (this) {
        BackupError.Format.MALFORMED -> R.string.settings_error_malformed

        BackupError.Format.NEWER_VERSION -> R.string.settings_error_newer

        BackupError.FileUnavailable -> R.string.settings_error_file

        is BackupError.Storage -> when (error) {
            DataError.Local.DISK_FULL -> R.string.settings_error_disk_full
            DataError.Local.UNKNOWN -> R.string.settings_error_storage
        }
    }
)
