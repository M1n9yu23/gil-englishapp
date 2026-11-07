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
package com.gyugle.gil.feature.reminder.impl.data.di

import com.gyugle.gil.core.domain.ApplicationScope
import com.gyugle.gil.feature.reminder.impl.data.Reminders
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

/**
 * How the receivers reach the graph. `@AndroidEntryPoint` on a receiver needs a call to the
 * abstract `super.onReceive`, which Kotlin does not compile.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ReminderReceiverEntryPoint {
    fun reminders(): Reminders

    @ApplicationScope
    fun applicationScope(): CoroutineScope
}
