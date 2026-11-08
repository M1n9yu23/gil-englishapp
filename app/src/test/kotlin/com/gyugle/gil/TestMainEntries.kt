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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.designsystem.theme.Spacing
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.feature.curriculum.api.PathRoute
import com.gyugle.gil.feature.dictionary.api.WordsRoute
import com.gyugle.gil.feature.home.api.TodayRoute
import com.gyugle.gil.feature.stats.api.StatsHomeRoute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val TEST_TODAY_ROOT = "Today root"
const val TEST_PATH_ROOT = "Path root"
const val TEST_OPEN_PAGE = "Open page"
const val TEST_PAGE = "Pushed page"
const val TEST_WORDS_ROOT = "Words root"
const val TEST_STATS_ROOT = "Stats root"

@Serializable
@SerialName("test.page")
data object TestPageRoute : NavKey

/**
 * The main host's tab roots as stand-ins that need no DI graph. The Path root's button pushes
 * [TestPageRoute] onto the Path stack. The Today root starts at the screen margin, like the real
 * one, unless [today] draws it; [stats] draws the Stats root.
 */
fun EntryProviderScope<NavKey>.testMainEntries(
    navigator: Navigator,
    today: @Composable () -> Unit = {
        Text(TEST_TODAY_ROOT, modifier = Modifier.fillMaxSize().padding(start = Spacing().screenMargin))
    },
    stats: @Composable () -> Unit = { Text(TEST_STATS_ROOT, modifier = Modifier.fillMaxSize()) }
) {
    entry<TodayRoute> { today() }
    entry<PathRoute> {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(TEST_PATH_ROOT)
            Text(TEST_OPEN_PAGE, modifier = Modifier.clickable { navigator.goTo(TestPageRoute) })
        }
    }
    entry<TestPageRoute> { Text(TEST_PAGE, modifier = Modifier.fillMaxSize()) }
    entry<WordsRoute> { Text(TEST_WORDS_ROOT, modifier = Modifier.fillMaxSize()) }
    entry<StatsHomeRoute> { stats() }
}
