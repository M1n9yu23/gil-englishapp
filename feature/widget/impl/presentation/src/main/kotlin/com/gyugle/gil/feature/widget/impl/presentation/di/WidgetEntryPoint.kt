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
package com.gyugle.gil.feature.widget.impl.presentation.di

import com.gyugle.gil.core.domain.ApplicationScope
import com.gyugle.gil.feature.progress.api.StudyClock
import com.gyugle.gil.feature.widget.impl.domain.TodayWidgetReader
import com.gyugle.gil.feature.widget.impl.presentation.DayRefreshAlarm
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * How the widget and its receivers reach the graph: Glance creates the widget itself, and
 * `@AndroidEntryPoint` on a receiver needs a call to the abstract `super.onReceive`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface WidgetEntryPoint {
    fun reader(): TodayWidgetReader

    fun clock(): StudyClock

    fun dayRefreshAlarm(): DayRefreshAlarm

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
