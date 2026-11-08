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
package com.gyugle.gil.feature.listening.impl.data.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.gyugle.gil.core.datastore.DataStoreFactory
import com.gyugle.gil.feature.listening.impl.data.DataStoreListeningPreferences
import com.gyugle.gil.feature.listening.impl.data.TtsTrackRenderer
import com.gyugle.gil.feature.listening.impl.data.playback.MediaControllerListeningPlayer
import com.gyugle.gil.feature.listening.impl.domain.ListeningPlayer
import com.gyugle.gil.feature.listening.impl.domain.ListeningPreferences
import com.gyugle.gil.feature.listening.impl.domain.TrackRenderer
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface ListeningDataModule {
    @Binds
    fun trackRenderer(renderer: TtsTrackRenderer): TrackRenderer

    @Binds
    fun listeningPlayer(player: MediaControllerListeningPlayer): ListeningPlayer

    @Binds
    fun listeningPreferences(preferences: DataStoreListeningPreferences): ListeningPreferences

    companion object {
        /** Left out of backups on purpose: a pace and a resume point are not worth restoring. */
        @Provides
        @Singleton
        @ListeningPrefs
        fun listeningPrefs(factory: DataStoreFactory): DataStore<Preferences> = factory.create("listening_prefs")
    }
}
