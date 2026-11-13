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

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.gyugle.gil.core.domain.BackupError
import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.domain.flatMap
import com.gyugle.gil.core.domain.map
import com.gyugle.gil.core.domain.mapError
import com.gyugle.gil.feature.goal.api.GoalCommands
import com.gyugle.gil.feature.goal.api.GoalObserver
import com.gyugle.gil.feature.progress.api.ProgressBackup
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.reminder.api.ReminderCommands
import com.gyugle.gil.feature.reminder.api.ReminderObserver
import com.gyugle.gil.feature.settings.impl.data.backup.BackupContents
import com.gyugle.gil.feature.settings.impl.data.backup.BackupDocuments
import com.gyugle.gil.feature.settings.impl.data.backup.BackupFile
import com.gyugle.gil.feature.settings.impl.data.backup.BackupFileCodec
import com.gyugle.gil.feature.settings.impl.data.backup.BackupSnapshots
import com.gyugle.gil.feature.settings.impl.domain.model.BackupPreview
import com.gyugle.gil.feature.settings.impl.domain.repository.BackupRepository
import com.gyugle.gil.feature.settings.impl.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Writes and restores backup files. A restore checks the whole file first, then saves a snapshot of
 * what it replaces, then writes the record, the goal, the reminders and the settings in that order.
 * If one of them fails, the snapshot goes back in, so the learner never keeps half of a file.
 *
 * @param appVersionCode the version written into each file.
 * @param jsonDispatcher runs the encoding and parsing, which can take a while for a long record.
 */
@Singleton
internal class FileBackupRepository internal constructor(
    private val documents: BackupDocuments,
    private val snapshots: BackupSnapshots,
    private val progress: ProgressBackup,
    private val goalObserver: GoalObserver,
    private val goalCommands: GoalCommands,
    private val reminderObserver: ReminderObserver,
    private val reminderCommands: ReminderCommands,
    private val settings: SettingsRepository,
    private val clock: StudyClock,
    private val appVersionCode: () -> Long,
    private val jsonDispatcher: CoroutineDispatcher
) : BackupRepository {

    @Inject
    constructor(
        documents: BackupDocuments,
        snapshots: BackupSnapshots,
        progress: ProgressBackup,
        goalObserver: GoalObserver,
        goalCommands: GoalCommands,
        reminderObserver: ReminderObserver,
        reminderCommands: ReminderCommands,
        settings: SettingsRepository,
        clock: StudyClock,
        @ApplicationContext context: Context
    ) : this(
        documents,
        snapshots,
        progress,
        goalObserver,
        goalCommands,
        reminderObserver,
        reminderCommands,
        settings,
        clock,
        { PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)) },
        Dispatchers.Default
    )

    /** One export or restore at a time, so a restore never reads what another is writing. */
    private val mutex = Mutex()

    override suspend fun export(uri: String): EmptyOutcome<BackupError> = mutex.withLock {
        current().flatMap { contents ->
            val text = encode(contents)
            if (documents.write(uri, text)) Outcome.Success(Unit) else Outcome.Failure(BackupError.FileUnavailable)
        }
    }

    override suspend fun preview(uri: String): Outcome<BackupPreview, BackupError> = read(uri).flatMap { file ->
        progress.preview(file.contents.progressSection).map { record ->
            BackupPreview(
                createdAt = file.createdAt,
                itemCount = record.itemCount,
                completedLessonCount = record.completedLessonCount,
                lastStudyDay = record.lastStudyDay
            )
        }
    }

    override suspend fun restore(uri: String): EmptyOutcome<BackupError> = mutex.withLock {
        read(uri)
            // The record's own check, before anything is replaced.
            .flatMap { file -> progress.preview(file.contents.progressSection).map { file.contents } }
            .flatMap { incoming ->
                current().flatMap { before ->
                    if (!snapshots.save(encode(before))) return@withLock Outcome.Failure(SNAPSHOT_FAILED)
                    // Leaving the screen must not stop a restore halfway or skip the rollback.
                    withContext(NonCancellable) {
                        val result = write(incoming)
                        if (result is Outcome.Failure) write(before)
                        result
                    }
                }
            }
    }

    override suspend fun deleteSnapshot() = mutex.withLock { snapshots.delete() }

    private suspend fun read(uri: String): Outcome<BackupFile, BackupError> {
        val text = documents.read(uri) ?: return Outcome.Failure(BackupError.FileUnavailable)
        return withContext(jsonDispatcher) { BackupFileCodec.decode(text) }
    }

    private suspend fun current(): Outcome<BackupContents, BackupError> =
        progress.export().mapError(BackupError::Storage).map { section ->
            BackupContents(
                progressSection = section,
                goal = goalObserver.observeGoal().first(),
                reminders = reminderObserver.observeTimes().first(),
                settings = settings.observeSettings().first()
            )
        }

    private suspend fun encode(contents: BackupContents): String = withContext(jsonDispatcher) {
        BackupFileCodec.encode(BackupFile(clock.now(), appVersionCode(), contents))
    }

    private suspend fun write(contents: BackupContents): EmptyOutcome<BackupError> =
        progress.restore(contents.progressSection)
            .flatMap { goalCommands.setGoal(contents.goal).mapError(BackupError::Storage) }
            .flatMap { reminderCommands.setTimes(contents.reminders).mapError(BackupError::Storage) }
            .flatMap { settings.setAll(contents.settings).mapError(BackupError::Storage) }

    private companion object {
        val SNAPSHOT_FAILED = BackupError.Storage(DataError.Local.UNKNOWN)
    }
}
