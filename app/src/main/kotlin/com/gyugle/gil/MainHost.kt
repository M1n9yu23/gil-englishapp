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
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.navigation.NavigationState
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.core.navigation.rememberNavigationState

private val destinations = TopLevelDestination.entries

/** The tabbed part of the app: one back stack per tab, and the bar only on a tab's root. */
@Composable
fun MainHost(modifier: Modifier = Modifier) {
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
        val fade = GilTheme.motion.fade
        // Every transition fades, including a pushed screen's. Tab switches should fade (they are
        // not forward or back); a slide for pushed screens must leave them alone.
        val transition: ContentTransform = fadeIn(tween(fade)) togetherWith fadeOut(tween(fade))
        NavDisplay(
            entries = navigationState.toEntries(
                entryProvider {
                    tabRootPlaceholderEntries(navigator)
                }
            ),
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            transitionSpec = { transition },
            popTransitionSpec = { transition },
            predictivePopTransitionSpec = { transition },
            onBack = navigator::goBack
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
