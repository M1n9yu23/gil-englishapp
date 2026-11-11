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

import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.SceneStrategyScope
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class CenteredPaneSceneStrategyTest {
    private data object Today : NavKey

    private data object Settings : NavKey

    private val strategy = CenteredPaneSceneStrategy<NavKey>(maxWidth = 600.dp)
    private val scope = SceneStrategyScope<NavKey>()

    private fun entry(key: NavKey) = NavEntry(key = key, contentKey = key.toString()) {}

    @Test
    fun calculateScene_twoEntries_showsOnlyTheTopOne() {
        val today = entry(Today)
        val settings = entry(Settings)

        val scene = with(strategy) { scope.calculateScene(listOf(today, settings)) }

        assertThat(scene).isNotNull()
        assertThat(scene!!.entries).containsExactly(settings)
        assertThat(scene.previousEntries).containsExactly(today)
        assertThat(scene.key).isEqualTo("Settings")
    }

    @Test
    fun calculateScene_oneEntry_hasNothingToGoBackTo() {
        val scene = with(strategy) { scope.calculateScene(listOf(entry(Today))) }

        assertThat(scene!!.previousEntries).isEmpty()
    }

    @Test
    fun calculateScene_noEntries_returnsNull() {
        assertThat(with(strategy) { scope.calculateScene(emptyList()) }).isNull()
    }
}
