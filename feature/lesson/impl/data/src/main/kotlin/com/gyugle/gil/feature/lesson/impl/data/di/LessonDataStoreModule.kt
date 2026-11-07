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
package com.gyugle.gil.feature.lesson.impl.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gyugle.gil.core.datastore.DataStoreFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object LessonDataStoreModule {
    /** The name is listed in the app's backup rules, so a restored phone keeps its place in a lesson. */
    @Provides
    @Singleton
    @LessonPreferences
    fun lessonPreferences(factory: DataStoreFactory): DataStore<Preferences> = factory.create("lesson_prefs")
}
