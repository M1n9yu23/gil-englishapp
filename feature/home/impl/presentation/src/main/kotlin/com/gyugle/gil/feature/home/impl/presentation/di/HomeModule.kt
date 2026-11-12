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
package com.gyugle.gil.feature.home.impl.presentation.di

import com.gyugle.gil.feature.home.api.ContinueFinder
import com.gyugle.gil.feature.home.impl.presentation.TodayContinueFinder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Home keeps no data of its own, so its one public contract is bound next to the screen that plans. */
@Module
@InstallIn(SingletonComponent::class)
internal interface HomeModule {
    @Binds
    fun continueFinder(finder: TodayContinueFinder): ContinueFinder
}
