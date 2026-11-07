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

import androidx.navigation3.runtime.NavKey

/**
 * Moves around the back stacks in [state]. Mechanism only: where to go after an action is the
 * caller's decision. Created in composition and passed to entries, never put in the DI graph.
 */
class Navigator(val state: NavigationState) {

    /** Switches to a top-level route with its history kept, or pushes [route] on the current stack. */
    fun goTo(route: NavKey) {
        if (route in state.backStacks) {
            state.topLevelRoute = route
            return
        }
        val stack = state.currentStack
        if (stack.last() != route) stack.add(route)
    }

    /**
     * Pops the current stack. At the root of a tab other than the start one, returns to the start
     * tab. At the start tab's root it does nothing; `NavDisplay` leaves back handling to the system
     * there, which closes the app.
     */
    fun goBack() {
        val stack = state.currentStack
        if (stack.size > 1) {
            stack.removeAt(stack.lastIndex)
        } else if (state.topLevelRoute != state.startRoute) {
            state.topLevelRoute = state.startRoute
        }
    }

    /**
     * Pushes [route] after removing everything above the last [popUpTo] in the current stack, and
     * [popUpTo] too when [inclusive]. The stack's root is always kept.
     */
    fun goToAndPopUpTo(route: NavKey, popUpTo: NavKey, inclusive: Boolean = false) {
        require(route !in state.backStacks) { "$route is a tab; switch to it with goTo" }
        val stack = state.currentStack
        val index = stack.indexOfLast { it == popUpTo }
        if (index >= 0) {
            val keep = (if (inclusive) index else index + 1).coerceAtLeast(1)
            if (keep < stack.size) stack.subList(keep, stack.size).clear()
        }
        stack.add(route)
    }

    /** Clears every tab back to its root and shows the start tab. */
    fun resetToStart() {
        state.backStacks.values.forEach { stack -> stack.trimToRoot() }
        state.topLevelRoute = state.startRoute
    }

    /**
     * Shows [target] from a clean tab: the tab's history is replaced by the target's routes.
     * Returns false and changes nothing when the target's tab is not a tab here, or when one of
     * its routes is a tab, which would put a tab's root inside another stack.
     */
    fun open(target: DeepLinkTarget): Boolean {
        val stack = state.backStacks[target.topLevelRoute] ?: return false
        if (target.routes.any { it in state.backStacks }) return false
        stack.trimToRoot()
        stack.addAll(target.routes)
        state.topLevelRoute = target.topLevelRoute
        return true
    }
}

private fun MutableList<NavKey>.trimToRoot() {
    if (size > 1) subList(1, size).clear()
}
