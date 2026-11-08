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
package com.gyugle.gil.feature.listening.impl.domain

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import kotlinx.coroutines.flow.Flow

/** Listening mode's own choices, and where the learner stopped. Not part of any backup. */
interface ListeningPreferences {
    /** The default pace until the learner changes it. */
    fun observePace(): Flow<ListeningPace>

    suspend fun setPace(pace: ListeningPace): EmptyOutcome<DataError.Local>

    /** The track the learner was on, null once a playlist was heard to the end or stopped there. */
    fun observeLastPlayed(): Flow<LastPlayed?>

    suspend fun setLastPlayed(lastPlayed: LastPlayed?): EmptyOutcome<DataError.Local>
}
