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
package com.gyugle.gil.feature.lesson.impl.presentation.player

/**
 * Whole minutes, rounded down like the time on the Today tab, and never less than one: a finished
 * lesson took some time.
 */
internal fun minutesOf(studiedMillis: Long): Int = maxOf(1, (studiedMillis / MILLIS_PER_MINUTE).toInt())

private const val MILLIS_PER_MINUTE = 60_000L
