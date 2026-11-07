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
package com.gyugle.gil.feature.curriculum.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface CurriculumRoute : NavKey

/** The Path tab: the stages and units of the course. */
@Serializable
@SerialName("curriculum.path")
data object PathRoute : CurriculumRoute

/** One unit and its lessons, opened from the path map. */
@Serializable
@SerialName("curriculum.unit")
data class UnitRoute(val unitId: UnitId) : CurriculumRoute
