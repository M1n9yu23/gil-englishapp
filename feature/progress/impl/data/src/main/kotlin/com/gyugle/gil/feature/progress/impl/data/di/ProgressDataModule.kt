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
package com.gyugle.gil.feature.progress.impl.data.di

import com.gyugle.gil.feature.progress.api.ProgressBackup
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.progress.impl.data.SystemStudyClock
import com.gyugle.gil.feature.progress.impl.data.repository.RoomProgressBackup
import com.gyugle.gil.feature.progress.impl.data.repository.RoomProgressRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ProgressDataModule {
    @Binds
    fun progressObserver(repository: RoomProgressRepository): ProgressObserver

    @Binds
    fun progressCommands(repository: RoomProgressRepository): ProgressCommands

    @Binds
    fun progressBackup(backup: RoomProgressBackup): ProgressBackup

    @Binds
    fun studyClock(clock: SystemStudyClock): StudyClock
}
