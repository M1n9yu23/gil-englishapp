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
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.settings.impl.domain.model.BackupPreview
import com.gyugle.gil.feature.settings.impl.domain.repository.FakeBackupRepository
import com.gyugle.gil.feature.settings.impl.presentation.R
import java.util.TimeZone
import kotlin.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BackupViewModelTest {
    private val backup = FakeBackupRepository()
    private val handle = SavedStateHandle()
    private val clock = FakeStudyClock(Instant.parse("2026-11-07T15:30:00Z"))
    private val preview = BackupPreview(Instant.parse("2026-11-01T09:00:00Z"), 12, 3, StudyDay(20393))
    private val defaultZone = TimeZone.getDefault()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"))
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
        TimeZone.setDefault(defaultZone)
    }

    private fun viewModel() = BackupViewModel(handle, backup, clock)

    @Test
    fun export_namesTheFileByTheLocalDayAndSaysWhenSaved() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onAction(BackupAction.OnExportClick)
            // 15:30 UTC is already 00:30 the next day in Seoul.
            assertThat(awaitItem()).isEqualTo(BackupEvent.PickExportFile("gil-backup-2026-11-08.json"))
            viewModel.onAction(BackupAction.OnExportPicked(FILE))
            assertThat(
                awaitItem()
            ).isEqualTo(BackupEvent.ShowMessage(UiText.StringResource(R.string.settings_exported)))
        }
        assertThat(backup.exported).containsExactly(FILE)
        assertThat(viewModel.state.value).isEqualTo(BackupState.Idle)
    }

    @Test
    fun export_pickerCancelled_doesNothing() {
        val viewModel = viewModel()

        viewModel.onAction(BackupAction.OnExportPicked(null))

        assertThat(backup.exported).isEmpty()
        assertThat(viewModel.state.value).isEqualTo(BackupState.Idle)
    }

    @Test
    fun export_whileSaving_showsExportingThenTheFailure() {
        val pending = CompletableDeferred<Unit>()
        backup.pending = pending
        backup.exportResult = Outcome.Failure(BackupError.FileUnavailable)
        val viewModel = viewModel()

        viewModel.onAction(BackupAction.OnExportPicked(FILE))
        assertThat(viewModel.state.value).isEqualTo(BackupState.Exporting)
        pending.complete(Unit)

        assertThat(viewModel.state.value).isEqualTo(
            BackupState.Failed(BackupStep.EXPORT, UiText.StringResource(R.string.settings_error_file))
        )
    }

    @Test
    fun import_showsTheSummaryThenRestores() {
        backup.previewResult = Outcome.Success(preview)
        val viewModel = viewModel()

        viewModel.onAction(BackupAction.OnImportPicked(FILE))
        assertThat(viewModel.state.value).isEqualTo(BackupState.Preview(preview))
        assertThat(backup.restored).isEmpty()

        viewModel.onAction(BackupAction.OnRestoreClick)

        assertThat(backup.restored).containsExactly(FILE)
        assertThat(viewModel.state.value).isEqualTo(BackupState.Restored)
    }

    @Test
    fun import_cancelledAtTheSummary_restoresNothing() {
        backup.previewResult = Outcome.Success(preview)
        val viewModel = viewModel()
        viewModel.onAction(BackupAction.OnImportPicked(FILE))

        viewModel.onAction(BackupAction.OnDoneClick)

        assertThat(backup.restored).isEmpty()
        assertThat(viewModel.state.value).isEqualTo(BackupState.Idle)
    }

    @Test
    fun import_newerFile_saysToUpdate() {
        backup.previewResult = Outcome.Failure(BackupError.Format.NEWER_VERSION)
        val viewModel = viewModel()

        viewModel.onAction(BackupAction.OnImportPicked(FILE))

        assertThat(viewModel.state.value).isEqualTo(
            BackupState.Failed(BackupStep.READ, UiText.StringResource(R.string.settings_error_newer))
        )
    }

    @Test
    fun restore_fails_saysTheRecordIsUnchanged() {
        backup.previewResult = Outcome.Success(preview)
        backup.restoreResult = Outcome.Failure(BackupError.Storage(DataError.Local.DISK_FULL))
        val viewModel = viewModel()
        viewModel.onAction(BackupAction.OnImportPicked(FILE))

        viewModel.onAction(BackupAction.OnRestoreClick)

        assertThat(viewModel.state.value).isEqualTo(
            BackupState.Failed(
                BackupStep.RESTORE,
                UiText.StringResource(R.string.settings_error_disk_full),
                unchanged = true
            )
        )
    }

    @Test
    fun summary_afterProcessDeath_isReadAgain() {
        backup.previewResult = Outcome.Success(preview)
        viewModel().onAction(BackupAction.OnImportPicked(FILE))

        val restored = viewModel()

        assertThat(backup.previewed).containsExactly(FILE, FILE)
        assertThat(restored.state.value).isEqualTo(BackupState.Preview(preview))
    }

    private companion object {
        const val FILE = "content://documents/gil-backup.json"
    }
}
