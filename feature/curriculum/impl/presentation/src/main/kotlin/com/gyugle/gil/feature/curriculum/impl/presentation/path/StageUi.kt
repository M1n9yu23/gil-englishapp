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
package com.gyugle.gil.feature.curriculum.impl.presentation.path

import androidx.compose.runtime.Immutable
import com.gyugle.gil.feature.curriculum.impl.presentation.LocalizedTextUi

@Immutable
data class StageUi(
    val id: String,
    val title: LocalizedTextUi,
    val canDo: List<LocalizedTextUi>,
    val units: List<UnitRowUi>,
    val place: StagePlace
)
