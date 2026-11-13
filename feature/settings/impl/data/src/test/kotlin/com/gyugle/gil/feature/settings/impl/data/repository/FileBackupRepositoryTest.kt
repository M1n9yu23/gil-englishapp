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
package com.gyugle.gil.feature.settings.impl.data.repository

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.goal.api.DailyGoal
import com.gyugle.gil.feature.goal.api.FakeGoalRepository
import com.gyugle.gil.feature.progress.api.FakeProgressBackup
import com.gyugle.gil.feature.progress.api.FakeStudyClock
import com.gyugle.gil.feature.progress.api.ProgressBackupPreview
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.reminder.api.FakeReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderTime
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import com.gyugle.gil.feature.settings.api.ThemeMode
import com.gyugle.gil.feature.settings.impl.data.backup.FakeBackupDocuments
import com.gyugle.gil.feature.settings.impl.data.backup.FakeBackupSnapshots
import com.gyugle.gil.feature.settings.impl.domain.model.BackupPreview
import com.gyugle.gil.feature.settings.impl.domain.repository.FakeSettingsRepository
import kotlin.time.Instant
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * Restores the committed example of each format version and writes it out again. A change that
 * breaks a saved file fails here; a new format version adds its own example file.
 */
class FileBackupRepositoryTest {
    private val versionOne = readResource("backup/gil-backup-v1.json")

    /** The progress section of [versionOne] as the progress feature receives it. */
    private val recordOne = Json.encodeToString(
        JsonObject.serializer(),
        Json.parseToJsonElement(versionOne).jsonObject.getValue("sections").jsonObject.getValue("progress").jsonObject
    )
    private val recordOnePreview = ProgressBackupPreview(1, 1, StudyDay(20762))

    private val documents = FakeBackupDocuments().apply { files[FILE] = versionOne }
    private val snapshots = FakeBackupSnapshots()
    private val progress = FakeProgressBackup().apply { previews[recordOne] = recordOnePreview }
    private val goal = FakeGoalRepository(DailyGoal.TEN)
    private val reminders = FakeReminderCommands().apply { times.value = listOf(ReminderTime(20, 0)) }
    private val settings = FakeSettingsRepository()
    private val clock = FakeStudyClock(Instant.parse("2026-11-08T09:30:00Z"))

    private fun TestScope.repository() = FileBackupRepository(
        documents = documents,
        snapshots = snapshots,
        progress = progress,
        goalObserver = goal,
        goalCommands = goal,
        reminderObserver = reminders,
        reminderCommands = reminders,
        settings = settings,
        clock = clock,
        appVersionCode = { 2 },
        jsonDispatcher = StandardTestDispatcher(testScheduler)
    )

    @Test
    fun restoreThenExport_versionOneFile_writesTheSameFileBack() = runTest {
        val repository = repository()

        assertThat(repository.restore(FILE)).isEqualTo(Outcome.Success(Unit))
        assertThat(repository.export(COPY)).isEqualTo(Outcome.Success(Unit))

        assertThat(Json.parseToJsonElement(documents.files.getValue(COPY)))
            .isEqualTo(Json.parseToJsonElement(versionOne))
    }

    @Test
    fun restore_versionOneFile_replacesRecordGoalRemindersAndSettings() = runTest {
        repository().restore(FILE)

        assertThat(progress.record).isEqualTo(recordOne)
        assertThat(goal.goal.value).isEqualTo(DailyGoal.THIRTY)
        assertThat(reminders.times.value).containsExactly(ReminderTime(7, 45), ReminderTime(21, 0))
        assertThat(settings.settings.value).isEqualTo(
            AppSettings(ThemeMode.DARK, Accent.BRITISH, SpeechSpeed.SLOW, showHangulAid = false)
        )
    }

    @Test
    fun restore_savesWhatItReplacesBeforeWriting() = runTest {
        repository().restore(FILE)

        val snapshot = Json.parseToJsonElement(snapshots.saved.single()).jsonObject.getValue("sections").jsonObject
        assertThat(snapshot.getValue("progress").toString()).isEqualTo(FakeProgressBackup.EMPTY)
        assertThat(snapshot.getValue("goal").toString()).isEqualTo("""{"version":1,"minutes":10}""")
        assertThat(snapshot.getValue("reminders").toString()).isEqualTo("""{"version":1,"times":["20:00"]}""")
    }

    @Test
    fun deleteSnapshot_afterRestore_removesTheCopy() = runTest {
        val repository = repository()
        repository.restore(FILE)

        repository.deleteSnapshot()

        assertThat(snapshots.saved).isEmpty()
    }

    @Test
    fun restore_lastPartFailsToWrite_putsEverythingBack() = runTest {
        settings.error = DataError.Local.DISK_FULL

        val result = repository().restore(FILE)

        assertThat(result).isEqualTo(Outcome.Failure(BackupError.Storage(DataError.Local.DISK_FULL)))
        assertThat(progress.record).isEqualTo(FakeProgressBackup.EMPTY)
        assertThat(goal.goal.value).isEqualTo(DailyGoal.TEN)
        assertThat(reminders.times.value).containsExactly(ReminderTime(20, 0))
        assertThat(settings.settings.value).isEqualTo(AppSettings())
    }

    @Test
    fun restore_snapshotCannotBeSaved_changesNothing() = runTest {
        snapshots.writable = false

        val result = repository().restore(FILE)

        assertThat(result).isEqualTo(Outcome.Failure(BackupError.Storage(DataError.Local.UNKNOWN)))
        assertThat(progress.restored).isEmpty()
        assertThat(goal.goal.value).isEqualTo(DailyGoal.TEN)
    }

    @Test
    fun restore_recordTheProgressFeatureRejects_changesNothing() = runTest {
        progress.previews.remove(recordOne)

        val result = repository().restore(FILE)

        assertThat(result).isEqualTo(Outcome.Failure(BackupError.Format.MALFORMED))
        assertThat(snapshots.saved).isEmpty()
        assertThat(goal.goal.value).isEqualTo(DailyGoal.TEN)
    }

    @Test
    fun preview_versionOneFile_summarizesWithoutWriting() = runTest {
        val result = repository().preview(FILE)

        assertThat(result).isEqualTo(
            Outcome.Success(BackupPreview(Instant.parse("2026-11-08T09:30:00Z"), 1, 1, StudyDay(20762)))
        )
        assertThat(progress.restored).isEmpty()
        assertThat(goal.goal.value).isEqualTo(DailyGoal.TEN)
    }

    @Test
    fun preview_fileCannotBeRead_isFileUnavailable() = runTest {
        assertThat(repository().preview("content://missing")).isEqualTo(Outcome.Failure(BackupError.FileUnavailable))
    }

    @Test
    fun export_fileCannotBeWritten_isFileUnavailable() = runTest {
        documents.writable = false

        assertThat(repository().export(COPY)).isEqualTo(Outcome.Failure(BackupError.FileUnavailable))
    }

    @Test
    fun export_recordCannotBeRead_isStorageError() = runTest {
        progress.exportError = DataError.Local.UNKNOWN

        assertThat(repository().export(COPY))
            .isEqualTo(Outcome.Failure(BackupError.Storage(DataError.Local.UNKNOWN)))
        assertThat(documents.files.keys).isEqualTo(setOf(FILE))
    }

    @Test
    fun export_writesEachTimeOnceInDayOrderWithLatinDigits() = runTest {
        reminders.times.value = listOf(ReminderTime(6, 5), ReminderTime(18, 30))

        repository().export(COPY)

        val section = Json.parseToJsonElement(documents.files.getValue(COPY)).jsonObject
            .getValue("sections").jsonObject.getValue("reminders").toString()
        assertThat(section).isEqualTo("""{"version":1,"times":["06:05","18:30"]}""")
    }

    @Test
    fun preview_newerFormatOrSection_isNewerVersion() = runTest {
        val repository = repository()
        val newer = listOf(
            versionOne.replace("\"formatVersion\": 1", "\"formatVersion\": 2"),
            versionOne.replace("\"goal\": { \"version\": 1", "\"goal\": { \"version\": 2"),
            versionOneWith("settings", "version", JsonPrimitive(2))
        )
        newer.forEachIndexed { index, text ->
            documents.files[FILE] = text
            assertThat(repository.preview(FILE), "case $index")
                .isEqualTo(Outcome.Failure(BackupError.Format.NEWER_VERSION))
        }
    }

    @Test
    fun restore_brokenFiles_areMalformedAndChangeNothing() = runTest {
        val repository = repository()
        val broken = listOf(
            "",
            "not json",
            "[]",
            versionOne.replace("\"gil-backup\"", "\"other-app\""),
            versionOne.replace("\"formatVersion\": 1", "\"formatVersion\": \"1\""),
            versionOne.replace("\"formatVersion\": 1", "\"formatVersion\": 0"),
            versionOne.replace("\"appVersionCode\": 2,", "\"appVersionCode\": 2, \"extra\": 1,"),
            versionOne.replace("2026-11-08T09:30:00Z", "yesterday"),
            versionOne.replace("\"goal\": { \"version\": 1, \"minutes\": 30 },", ""),
            versionOne.replace("\"goal\":", "\"streak\": { \"version\": 1 }, \"goal\":"),
            versionOne.replace("\"minutes\": 30", "\"minutes\": 45"),
            versionOne.replace("\"07:45\"", "\"24:00\""),
            versionOne.replace("\"07:45\"", "\"7:45\""),
            versionOne.replace("\"07:45\"", "\"21:00\""),
            versionOne.replace("\"07:45\", \"21:00\"", "\"07:00\", \"12:00\", \"21:00\""),
            versionOne.replace("\"dark\"", "\"sepia\""),
            versionOne.replace("\"showHangulAid\": false", "\"showHangulAid\": \"no\""),
            versionOne.replace("\"showHangulAid\": false", "\"showHangulAid\": false, \"font\": \"big\"")
        )
        broken.forEachIndexed { index, text ->
            documents.files[FILE] = text
            assertThat(repository.restore(FILE), "case $index")
                .isEqualTo(Outcome.Failure(BackupError.Format.MALFORMED))
        }
        assertThat(progress.restored).isEmpty()
        assertThat(snapshots.saved).isEmpty()
        assertThat(goal.goal.value).isEqualTo(DailyGoal.TEN)
    }

    /** [versionOne] with [key] of [section] set to [value]. */
    private fun versionOneWith(section: String, key: String, value: JsonElement): String {
        val root = Json.parseToJsonElement(versionOne).jsonObject
        val sections = root.getValue("sections").jsonObject
        val edited = JsonObject(sections.getValue(section).jsonObject + (key to value))
        return JsonObject(root + ("sections" to JsonObject(sections + (section to edited)))).toString()
    }

    private fun readResource(path: String): String =
        checkNotNull(javaClass.classLoader?.getResource(path)) { "Missing test resource $path" }.readText()

    private companion object {
        const val FILE = "content://documents/gil-backup.json"
        const val COPY = "content://documents/copy.json"
    }
}
