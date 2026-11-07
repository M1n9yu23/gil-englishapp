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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSerializable
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.serialization.NavKeySerializer
import androidx.savedstate.compose.serialization.serializers.MutableStateSerializer

/**
 * Creates the navigation state and keeps it across configuration changes and process death.
 *
 * Every route must be `@Serializable` with an explicit `@SerialName`, or a restore after
 * process death fails. A host with a single stack, such as onboarding, passes only its start route.
 */
@Composable
fun rememberNavigationState(startRoute: NavKey, vararg otherTopLevelRoutes: NavKey): NavigationState {
    val routes = listOf(startRoute) + otherTopLevelRoutes
    require(routes.distinct().size == routes.size) { "Top-level routes must be distinct: $routes" }

    // A different set of routes gets fresh state instead of stacks saved for other roots.
    return key(routes) {
        val topLevelRoute = rememberSerializable(serializer = MutableStateSerializer(NavKeySerializer())) {
            mutableStateOf(startRoute)
        }
        val backStacks = routes.associateWith { route -> rememberNavBackStack(route) }
        remember { NavigationState(startRoute, topLevelRoute, backStacks) }
    }
}
