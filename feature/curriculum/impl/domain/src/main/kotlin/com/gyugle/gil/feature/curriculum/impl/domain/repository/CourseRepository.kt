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
package com.gyugle.gil.feature.curriculum.impl.domain.repository

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.domain.model.Stage
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitDetail

/**
 * The course structure from the content database. The content ships with the app and never
 * changes while it runs, so these are one-shot reads rather than flows.
 */
interface CourseRepository {
    /** Every stage in course order, each with its units. [ContentError.UNAVAILABLE] when reading fails. */
    suspend fun getStages(): Outcome<List<Stage>, ContentError>

    /** [ContentError.NOT_FOUND] when no unit has [id]; [ContentError.UNAVAILABLE] when reading fails. */
    suspend fun getUnit(id: UnitId): Outcome<UnitDetail, ContentError>
}
