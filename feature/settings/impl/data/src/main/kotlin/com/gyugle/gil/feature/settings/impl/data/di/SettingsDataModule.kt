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
package com.gyugle.gil.feature.settings.impl.data.di

import com.gyugle.gil.feature.settings.api.AppSettingsObserver
import com.gyugle.gil.feature.settings.impl.data.backup.BackupDocuments
import com.gyugle.gil.feature.settings.impl.data.backup.BackupSnapshots
import com.gyugle.gil.feature.settings.impl.data.backup.ContentResolverBackupDocuments
import com.gyugle.gil.feature.settings.impl.data.backup.FileBackupSnapshots
import com.gyugle.gil.feature.settings.impl.data.repository.DataStoreSettingsRepository
import com.gyugle.gil.feature.settings.impl.data.repository.FileBackupRepository
import com.gyugle.gil.feature.settings.impl.data.repository.SystemAppLanguageRepository
import com.gyugle.gil.feature.settings.impl.domain.repository.AppLanguageRepository
import com.gyugle.gil.feature.settings.impl.domain.repository.BackupRepository
import com.gyugle.gil.feature.settings.impl.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SettingsDataModule {
    @Binds
    fun appSettingsObserver(repository: DataStoreSettingsRepository): AppSettingsObserver

    @Binds
    fun settingsRepository(repository: DataStoreSettingsRepository): SettingsRepository

    @Binds
    fun appLanguageRepository(repository: SystemAppLanguageRepository): AppLanguageRepository

    @Binds
    fun backupRepository(repository: FileBackupRepository): BackupRepository

    @Binds
    fun backupDocuments(documents: ContentResolverBackupDocuments): BackupDocuments

    @Binds
    fun backupSnapshots(snapshots: FileBackupSnapshots): BackupSnapshots
}
