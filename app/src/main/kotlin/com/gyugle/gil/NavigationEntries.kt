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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.Scene
import com.gyugle.gil.core.navigation.NavigationState

/**
 * The entries `NavDisplay` shows, bottom first. Each tab decorates its own stack, so every tab keeps
 * its own saved state and ViewModels while another tab is showing.
 *
 * A screen such as a word page can sit in two tabs at once with the same key. Its content key is
 * prefixed with the tab, so the two copies never share saved state, a ViewModel or a scene.
 * The same key twice in one stack still collides.
 */
@Composable
fun NavigationState.toEntries(entryProvider: (NavKey) -> NavEntry<NavKey>): List<NavEntry<NavKey>> {
    val decorated = backStacks.mapValues { (topLevelRoute, stack) ->
        // The decorators key their saved state and ViewModel store by position in composition;
        // key() gives each tab its own position instead of one shared loop body.
        key(topLevelRoute) {
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator()
                ),
                entryProvider = { key -> entryProvider(key).inTab(key, topLevelRoute) }
            )
        }
    }
    return stacksInUse.flatMap { decorated.getValue(it) }
}

private const val TAB_METADATA_KEY = "gil.tab"

/** The tab of the scene's top entry, so a transition can tell a tab switch from a move in a tab. */
internal fun Scene<NavKey>.tab(): NavKey? = entries.lastOrNull()?.metadata?.get(TAB_METADATA_KEY) as NavKey?

private fun NavEntry<NavKey>.inTab(key: NavKey, topLevelRoute: NavKey): NavEntry<NavKey> {
    val entry = this
    return NavEntry(
        key = key,
        contentKey = "$topLevelRoute/$contentKey",
        metadata = metadata + (TAB_METADATA_KEY to topLevelRoute)
    ) {
        entry.Content()
    }
}
