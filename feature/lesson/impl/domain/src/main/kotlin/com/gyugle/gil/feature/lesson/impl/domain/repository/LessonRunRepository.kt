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
package com.gyugle.gil.feature.lesson.impl.domain.repository

import com.gyugle.gil.core.domain.DataError
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.lesson.impl.domain.model.LessonRun

/** The lessons left halfway, one saved run per lesson. */
interface LessonRunRepository {
    /** The saved run of [lessonId], or null when there is none or it cannot be read. */
    suspend fun get(lessonId: LessonId): LessonRun?

    suspend fun save(lessonId: LessonId, run: LessonRun): EmptyOutcome<DataError.Local>

    /** Forgets the run of [lessonId], when it is finished or started over. */
    suspend fun clear(lessonId: LessonId): EmptyOutcome<DataError.Local>
}
