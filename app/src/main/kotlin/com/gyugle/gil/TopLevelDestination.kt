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
package com.gyugle.gil

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.feature.curriculum.api.PathRoute
import com.gyugle.gil.feature.dictionary.api.WordsRoute
import com.gyugle.gil.feature.home.api.TodayRoute
import com.gyugle.gil.feature.stats.api.StatsHomeRoute

/** The tabs of the main host, in bar order. The first one is the start tab. */
enum class TopLevelDestination(val route: NavKey, @param:StringRes val label: Int) {
    Today(TodayRoute, R.string.main_tab_today),
    Path(PathRoute, R.string.main_tab_path),
    Words(WordsRoute, R.string.main_tab_words),
    Progress(StatsHomeRoute, R.string.main_tab_progress);

    @Composable
    fun icon(selected: Boolean): Painter = when (this) {
        Today -> if (selected) GilIcons.TodayFilled else GilIcons.Today
        Path -> if (selected) GilIcons.RouteFilled else GilIcons.Route
        Words -> if (selected) GilIcons.DictionaryFilled else GilIcons.Dictionary
        Progress -> if (selected) GilIcons.AnalyticsFilled else GilIcons.Analytics
    }
}
