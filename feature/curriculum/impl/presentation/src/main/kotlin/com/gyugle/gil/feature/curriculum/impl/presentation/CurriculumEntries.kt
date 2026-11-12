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
package com.gyugle.gil.feature.curriculum.impl.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.feature.curriculum.api.PathRoute
import com.gyugle.gil.feature.curriculum.api.UnitRoute
import com.gyugle.gil.feature.curriculum.impl.presentation.path.PathRoot
import com.gyugle.gil.feature.curriculum.impl.presentation.unitdetail.UnitDetailRoot
import com.gyugle.gil.feature.lesson.api.LessonRoute
import com.gyugle.gil.feature.review.api.StageTestRoute

fun EntryProviderScope<NavKey>.curriculumEntries(navigator: Navigator) {
    entry<PathRoute> {
        PathRoot(
            onUnitClick = { unitId -> navigator.goTo(UnitRoute(unitId)) },
            onTestClick = { stageId -> navigator.goTo(StageTestRoute(stageId)) }
        )
    }
    entry<UnitRoute> { route ->
        UnitDetailRoot(
            unitId = route.unitId,
            onBack = navigator::goBack,
            onLessonClick = { lessonId -> navigator.goTo(LessonRoute(lessonId)) }
        )
    }
}
