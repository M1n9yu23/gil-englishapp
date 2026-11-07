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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavMetadataKey
import androidx.navigation3.runtime.contains
import androidx.navigation3.runtime.metadata
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/**
 * Shows an entry marked with [bottomSheet] in a modal bottom sheet over the entries below it.
 * Swiping the sheet away or pressing back pops the entry. Adapted from the Navigation 3 recipes.
 */
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val entry = entries.lastOrNull() ?: return null
        if (BottomSheetKey !in entry.metadata) return null
        return BottomSheetScene(
            key = entry.contentKey,
            previousEntries = entries.dropLast(1),
            overlaidEntries = entries.dropLast(1),
            entry = entry,
            onBack = onBack
        )
    }

    companion object {
        /** Entry metadata that shows the entry as a bottom sheet. */
        fun bottomSheet(): Map<String, Any> = metadata { put(BottomSheetKey, true) }
    }
}

private data object BottomSheetKey : NavMetadataKey<Boolean>

@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val onBack: () -> Unit
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)

    override val content: @Composable () -> Unit = {
        // The sheet animates itself away before it asks to be dismissed.
        ModalBottomSheet(onDismissRequest = onBack, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
            entry.Content()
        }
    }
}
