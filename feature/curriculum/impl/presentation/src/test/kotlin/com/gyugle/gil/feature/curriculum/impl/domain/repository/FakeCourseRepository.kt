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
import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.domain.error.ContentError
import com.gyugle.gil.feature.curriculum.impl.domain.model.Stage
import com.gyugle.gil.feature.curriculum.impl.domain.model.UnitDetail
import kotlinx.coroutines.CompletableDeferred

/** Returns whatever the test sets. [gate], when set, holds every read until the test completes it. */
class FakeCourseRepository : CourseRepository {
    var stages: Outcome<List<Stage>, ContentError> = Outcome.Success(emptyList())
    var units: Map<UnitId, UnitDetail> = emptyMap()
    var unitError: ContentError? = null
    var gate: CompletableDeferred<Unit>? = null
    val requestedUnits = mutableListOf<UnitId>()

    override suspend fun getStages(): Outcome<List<Stage>, ContentError> {
        gate?.await()
        return stages
    }

    override suspend fun getUnit(id: UnitId): Outcome<UnitDetail, ContentError> {
        requestedUnits += id
        gate?.await()
        unitError?.let { return Outcome.Failure(it) }
        return units[id]?.let { Outcome.Success(it) } ?: Outcome.Failure(ContentError.NOT_FOUND)
    }
}
