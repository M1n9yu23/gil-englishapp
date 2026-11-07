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
package com.gyugle.gil.core.navigation

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/**
 * One back stack per top-level route, plus which one is showing.
 *
 * Each stack starts with its own top-level route, and that first entry is never removed:
 * `NavDisplay` cannot show an empty stack.
 */
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
) {
    init {
        require(startRoute in backStacks) { "The start route $startRoute has no back stack" }
        backStacks.forEach { (route, stack) ->
            require(stack.firstOrNull() == route) { "The back stack of $route must start with $route" }
        }
    }

    var topLevelRoute: NavKey by topLevelRoute

    /**
     * The stacks to show, bottom first. The start stack always stays underneath another tab, so
     * back from that tab's root returns to the start tab before leaving the app.
     */
    val stacksInUse: List<NavKey>
        get() = if (topLevelRoute == startRoute) listOf(startRoute) else listOf(startRoute, topLevelRoute)

    val currentStack: NavBackStack<NavKey>
        get() = backStacks.getValue(topLevelRoute)
}
