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

import androidx.compose.runtime.Immutable
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.settings.impl.domain.model.BackupPreview

@Immutable
sealed interface BackupState {
    /** The two ways in: save a file, or pick one to restore. */
    data object Idle : BackupState

    data object Exporting : BackupState

    data object Reading : BackupState

    /** The file is valid; nothing has changed yet. */
    data class Preview(val preview: BackupPreview) : BackupState

    data class Restoring(val preview: BackupPreview) : BackupState

    data object Restored : BackupState

    /** @property reason what went wrong; [unchanged] adds that the record is as it was. */
    data class Failed(val step: BackupStep, val reason: UiText, val unchanged: Boolean = false) : BackupState
}

enum class BackupStep { EXPORT, READ, RESTORE }

sealed interface BackupAction {
    data object OnExportClick : BackupAction

    /** The document the system picker created, or null when the learner backed out. */
    data class OnExportPicked(val uri: String?) : BackupAction

    data object OnImportClick : BackupAction

    data class OnImportPicked(val uri: String?) : BackupAction

    data object OnRestoreClick : BackupAction

    /** Leaves the summary, a result or a failure for the two ways in. */
    data object OnDoneClick : BackupAction
}

sealed interface BackupEvent {
    /** Opens the system picker to create [fileName]. */
    data class PickExportFile(val fileName: String) : BackupEvent

    data object PickImportFile : BackupEvent

    data class ShowMessage(val message: UiText) : BackupEvent
}
