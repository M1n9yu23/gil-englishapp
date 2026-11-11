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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.navigation.BottomSheetSceneStrategy
import com.gyugle.gil.core.navigation.CenteredPaneSceneStrategy
import com.gyugle.gil.core.navigation.DeepLinkTarget
import com.gyugle.gil.core.navigation.NavigationState
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.core.navigation.rememberNavigationState
import com.gyugle.gil.core.presentation.DeviceConfiguration
import com.gyugle.gil.core.presentation.currentDeviceConfiguration
import com.gyugle.gil.feature.curriculum.impl.presentation.curriculumEntries
import com.gyugle.gil.feature.dictionary.impl.presentation.dictionaryEntries
import com.gyugle.gil.feature.home.impl.presentation.homeEntries
import com.gyugle.gil.feature.lesson.impl.presentation.lessonEntries
import com.gyugle.gil.feature.listening.impl.presentation.listeningEntries
import com.gyugle.gil.feature.onboarding.impl.presentation.studyMethodEntries
import com.gyugle.gil.feature.review.impl.presentation.reviewEntries
import com.gyugle.gil.feature.settings.impl.presentation.settingsEntries
import com.gyugle.gil.feature.stats.impl.presentation.statsEntries

private val destinations = TopLevelDestination.entries

/**
 * The tabbed part of the app: one back stack per tab, and the bar only on a tab's root.
 * [deepLink], an entry from outside such as a reminder, replaces its tab's history when it arrives.
 * [entries] are the screens; tests pass their own so that they need no DI graph.
 */
@Composable
fun MainHost(
    modifier: Modifier = Modifier,
    deepLink: DeepLinkTarget? = null,
    onDeepLinkOpen: () -> Unit = {},
    entries: EntryProviderScope<NavKey>.(Navigator) -> Unit = { navigator -> mainEntries(navigator) }
) {
    val navigationState = rememberNavigationState(
        destinations.first().route,
        *destinations.drop(1).map { it.route }.toTypedArray()
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val currentOnDeepLinkOpen by rememberUpdatedState(onDeepLinkOpen)
    LaunchedEffect(deepLink, navigator) {
        if (deepLink != null) {
            navigator.open(deepLink)
            currentOnDeepLinkOpen()
        }
    }

    val configuration = currentDeviceConfiguration()
    // Only a phone held upright has the height to spare for a bar; anything wider puts the tabs
    // beside the screen. Either way they show on tab roots only, so a lesson gets the whole window.
    val showsTabs = navigationState.currentStack.size == 1
    val tabsBeside = when (configuration) {
        DeviceConfiguration.MOBILE_PORTRAIT -> false

        DeviceConfiguration.MOBILE_LANDSCAPE,
        DeviceConfiguration.TABLET_PORTRAIT,
        DeviceConfiguration.TABLET_LANDSCAPE,
        DeviceConfiguration.DESKTOP -> true
    }
    val onSelect: (TopLevelDestination) -> Unit = { navigator.goTo(it.route) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Screens pad themselves for the status bar. On a tab root the bar below pads for the
        // navigation bar; a pushed screen has no bar and pads for it itself.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (showsTabs && !tabsBeside) MainNavigationBar(navigationState, onSelect)
        }
    ) { innerPadding ->
        Row(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            if (showsTabs && tabsBeside) MainNavigationRail(navigationState, onSelect)
            val transitions = rememberStackTransitions()
            val contentMaxWidth = GilTheme.spacing.contentMaxWidth
            val sceneStrategies = remember(contentMaxWidth) {
                listOf(BottomSheetSceneStrategy<NavKey>(), CenteredPaneSceneStrategy(contentMaxWidth))
            }
            NavDisplay(
                entries = navigationState.toEntries(entryProvider { entries(navigator) }),
                sceneStrategies = sceneStrategies,
                modifier = Modifier
                    .weight(1f)
                    // Beside the rail, the screen is clear of the start edge, and nothing below
                    // it pads for the navigation bar.
                    .then(
                        if (showsTabs && tabsBeside) {
                            Modifier
                                .consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
                                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                        } else {
                            Modifier
                        }
                    ),
                transitionSpec = { transitions.between(initialState, targetState, back = false) },
                popTransitionSpec = { transitions.between(initialState, targetState, back = true) },
                predictivePopTransitionSpec = { _ -> transitions.between(initialState, targetState, back = true) },
                onBack = navigator::goBack
            )
        }
    }
}

private fun EntryProviderScope<NavKey>.mainEntries(navigator: Navigator) {
    homeEntries(navigator)
    curriculumEntries(navigator)
    lessonEntries(navigator)
    reviewEntries(navigator)
    dictionaryEntries(navigator)
    statsEntries(navigator)
    settingsEntries(navigator)
    studyMethodEntries(navigator)
    listeningEntries(navigator)
}

@Composable
private fun MainNavigationBar(state: NavigationState, onSelect: (TopLevelDestination) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    // The default indicator (secondaryContainer) almost vanishes on the bar's paper tone.
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colorScheme.onPrimaryContainer,
        selectedTextColor = colorScheme.onSurface,
        indicatorColor = colorScheme.primaryContainer,
        unselectedIconColor = colorScheme.onSurfaceVariant,
        unselectedTextColor = colorScheme.onSurfaceVariant
    )
    NavigationBar(containerColor = colorScheme.surfaceContainer) {
        destinations.forEach { destination ->
            val selected = destination.route == state.topLevelRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(destination) },
                colors = itemColors,
                // The label below names the tab, so the icon is not announced on its own.
                icon = { Icon(painter = destination.icon(selected), contentDescription = null) },
                label = { Text(stringResource(destination.label)) }
            )
        }
    }
}

@Composable
private fun MainNavigationRail(state: NavigationState, onSelect: (TopLevelDestination) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    // The same colours as the bar, for the same reason.
    val itemColors = NavigationRailItemDefaults.colors(
        selectedIconColor = colorScheme.onPrimaryContainer,
        selectedTextColor = colorScheme.onSurface,
        indicatorColor = colorScheme.primaryContainer,
        unselectedIconColor = colorScheme.onSurfaceVariant,
        unselectedTextColor = colorScheme.onSurfaceVariant
    )
    // Scrolls when a large font makes the four tabs taller than a phone held sideways.
    NavigationRail(containerColor = colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)
        ) {
            destinations.forEach { destination ->
                val selected = destination.route == state.topLevelRoute
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelect(destination) },
                    colors = itemColors,
                    icon = { Icon(painter = destination.icon(selected), contentDescription = null) },
                    label = { Text(stringResource(destination.label)) }
                )
            }
        }
    }
}
