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

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.navigation.NavigationState
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.core.navigation.rememberNavigationState
import com.gyugle.gil.feature.curriculum.impl.presentation.curriculumEntries

private val destinations = TopLevelDestination.entries

private object MainHostTokens {
    /** A short shift is enough to show direction; a full-width slide would read as travel. */
    val SlideDistance = 30.dp
}

/**
 * The tabbed part of the app: one back stack per tab, and the bar only on a tab's root.
 * [entries] are the screens; tests pass their own so that they need no DI graph.
 */
@Composable
fun MainHost(
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<NavKey>.(Navigator) -> Unit = { navigator -> mainEntries(navigator) }
) {
    val navigationState = rememberNavigationState(
        destinations.first().route,
        *destinations.drop(1).map { it.route }.toTypedArray()
    )
    val navigator = remember(navigationState) { Navigator(navigationState) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        // Screens pad themselves for the status bar. On a tab root the bar below pads for the
        // navigation bar; a pushed screen has no bar and pads for it itself.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (navigationState.currentStack.size == 1) {
                MainNavigationBar(navigationState, onSelect = { navigator.goTo(it.route) })
            }
        }
    ) { innerPadding ->
        val transitions = rememberMainTransitions()
        NavDisplay(
            entries = navigationState.toEntries(entryProvider { entries(navigator) }),
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            // A tab switch is neither forward nor back, so it fades; moving within a tab slides.
            transitionSpec = { if (initialState.tab() == targetState.tab()) transitions.push else transitions.fade },
            popTransitionSpec = { if (initialState.tab() == targetState.tab()) transitions.pop else transitions.fade },
            predictivePopTransitionSpec = { _ ->
                if (initialState.tab() == targetState.tab()) transitions.pop else transitions.fade
            },
            onBack = navigator::goBack
        )
    }
}

/** Every screen of the main host. Remove a tab's placeholder when its feature brings the root. */
private fun EntryProviderScope<NavKey>.mainEntries(navigator: Navigator) {
    curriculumEntries(navigator)
    tabRootPlaceholderEntries(navigator)
}

private class MainTransitions(val fade: ContentTransform, val push: ContentTransform, val pop: ContentTransform)

@Composable
private fun rememberMainTransitions(): MainTransitions {
    val motion = GilTheme.motion
    val distance = with(LocalDensity.current) { MainHostTokens.SlideDistance.roundToPx() }
    return remember(motion, distance) {
        val enter = tween<Float>(motion.enter, easing = motion.enterEasing)
        val exit = tween<Float>(motion.exit, easing = motion.exitEasing)
        val enterOffset = tween<IntOffset>(motion.enter, easing = motion.enterEasing)
        val exitOffset = tween<IntOffset>(motion.exit, easing = motion.exitEasing)
        MainTransitions(
            fade = fadeIn(tween(motion.fade)) togetherWith fadeOut(tween(motion.fade)),
            push = (slideInHorizontally(enterOffset) { distance } + fadeIn(enter)) togetherWith
                (slideOutHorizontally(exitOffset) { -distance } + fadeOut(exit)),
            pop = (slideInHorizontally(enterOffset) { -distance } + fadeIn(enter)) togetherWith
                (slideOutHorizontally(exitOffset) { distance } + fadeOut(exit))
        )
    }
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
