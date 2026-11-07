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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.designsystem.component.EmptyState
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.feature.curriculum.api.PathRoute
import com.gyugle.gil.feature.home.api.TodayRoute
import com.gyugle.gil.feature.stats.api.StatsHomeRoute

/**
 * Stand-ins for the tab roots until each feature brings its own entries. Each one is the empty
 * state its real screen shows to a learner who has not started yet. Remove a tab's entry here when
 * its feature adds one.
 */
fun EntryProviderScope<NavKey>.tabRootPlaceholderEntries(navigator: Navigator) {
    entry<TodayRoute> {
        TabRootPlaceholder(
            title = R.string.main_tab_today,
            emptyTitle = R.string.home_today_empty_title,
            emptyBody = R.string.home_today_empty_body,
            action = R.string.home_today_empty_action,
            onAction = { navigator.goTo(PathRoute) }
        )
    }
    entry<StatsHomeRoute> {
        TabRootPlaceholder(
            title = R.string.main_tab_progress,
            emptyTitle = R.string.stats_home_empty_title,
            emptyBody = R.string.stats_home_empty_body
        )
    }
}

@Composable
private fun TabRootPlaceholder(
    @StringRes title: Int,
    @StringRes emptyTitle: Int,
    @StringRes emptyBody: Int,
    modifier: Modifier = Modifier,
    @StringRes action: Int? = null,
    onAction: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(title))
        EmptyState(
            title = stringResource(emptyTitle),
            body = stringResource(emptyBody),
            actionLabel = action?.let { stringResource(it) },
            onAction = onAction
        )
    }
}

@PreviewLightDark
@Composable
private fun TabRootPlaceholderPreview() {
    GilTheme {
        Surface {
            TabRootPlaceholder(
                title = R.string.main_tab_today,
                emptyTitle = R.string.home_today_empty_title,
                emptyBody = R.string.home_today_empty_body,
                action = R.string.home_today_empty_action
            )
        }
    }
}
