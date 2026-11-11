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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavEntry
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

    val transitions = rememberStackTransitions()
    val contentMaxWidth = GilTheme.spacing.contentMaxWidth
    val sceneStrategies = remember(contentMaxWidth) {
        listOf(BottomSheetSceneStrategy<NavKey>(), CenteredPaneSceneStrategy(contentMaxWidth))
    }
    // Measured rather than assumed, so a tab root clears the bar at any font size.
    var barHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current
    // A tab root keeps clear of the bar, or of the navigation bar when the tabs are beside it.
    // Screens pushed over it pad for the system bars themselves.
    val navigationBarPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues()
    val tabRootPadding by rememberUpdatedState(
        if (tabsBeside) navigationBarPadding else PaddingValues(bottom = barHeight)
    )
    val provider = entryProvider { entries(navigator) }
    val tabRoutes = remember { destinations.map { it.route }.toSet() }

    // The surface also sets the text colour that screens without their own surface draw in.
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (showsTabs && tabsBeside) MainNavigationRail(navigationState, onSelect)
                NavDisplay(
                    entries = navigationState.toEntries { key ->
                        val entry = provider(key)
                        if (key in tabRoutes) entry.paddedBy(key) { tabRootPadding } else entry
                    },
                    sceneStrategies = sceneStrategies,
                    modifier = Modifier
                        .weight(1f)
                        // Beside the rail, the screen is clear of the start edge.
                        .then(
                            if (showsTabs && tabsBeside) {
                                Modifier.consumeWindowInsets(WindowInsets.safeDrawing.only(WindowInsetsSides.Start))
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
            // Over the screens instead of beside them, so the screens never change size as it comes
            // and goes. It leaves and arrives with the tab root, before the next screen shows.
            AnimatedVisibility(
                visible = showsTabs && !tabsBeside,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = transitions.arrive,
                exit = transitions.leave
            ) {
                MainNavigationBar(
                    state = navigationState,
                    onSelect = onSelect,
                    modifier = Modifier.onSizeChanged { barHeight = with(density) { it.height.toDp() } }
                )
            }
        }
    }
}

/** The entry with [padding] around its content, consumed so the screen does not pad for it again. */
private fun NavEntry<NavKey>.paddedBy(key: NavKey, padding: () -> PaddingValues): NavEntry<NavKey> {
    val entry = this
    return NavEntry(key = key, contentKey = entry.contentKey, metadata = entry.metadata) {
        val values = padding()
        Box(
            modifier = Modifier
                .padding(values)
                .consumeWindowInsets(values)
        ) {
            entry.Content()
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
private fun MainNavigationBar(
    state: NavigationState,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    // The default indicator (secondaryContainer) almost vanishes on the bar's paper tone.
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = colorScheme.onPrimaryContainer,
        selectedTextColor = colorScheme.onSurface,
        indicatorColor = colorScheme.primaryContainer,
        unselectedIconColor = colorScheme.onSurfaceVariant,
        unselectedTextColor = colorScheme.onSurfaceVariant
    )
    NavigationBar(modifier = modifier, containerColor = colorScheme.surfaceContainer) {
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
