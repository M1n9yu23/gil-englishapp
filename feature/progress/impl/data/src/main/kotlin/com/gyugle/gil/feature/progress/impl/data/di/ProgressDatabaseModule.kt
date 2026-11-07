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

import android.content.Context
import androidx.room.Room
import com.gyugle.gil.core.database.applyProjectDefaults
import com.gyugle.gil.feature.progress.impl.data.ProgressDatabase
import com.gyugle.gil.feature.progress.impl.domain.FsrsScheduler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object ProgressDatabaseModule {
    /** Room opens the file on first use, off the main thread, not here. */
    @Provides
    @Singleton
    fun progressDatabase(@ApplicationContext context: Context): ProgressDatabase =
        Room.databaseBuilder(context, ProgressDatabase::class.java, ProgressDatabase.FILE_NAME)
            .applyProjectDefaults()
            .addMigrations(*ProgressDatabase.MIGRATIONS)
            .build()

    @Provides
    fun fsrsScheduler(): FsrsScheduler = FsrsScheduler()
}
