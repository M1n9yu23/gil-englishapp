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

import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.SceneStrategyScope
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import org.junit.jupiter.api.Test

class BottomSheetSceneStrategyTest {
    private data object Word : NavKey

    private data object Sentence : NavKey

    private val strategy = BottomSheetSceneStrategy<NavKey>()
    private val scope = SceneStrategyScope<NavKey>()

    private fun entry(key: NavKey, metadata: Map<String, Any> = emptyMap()) =
        NavEntry(key = key, contentKey = key.toString(), metadata = metadata) {}

    @Test
    fun calculateScene_topEntryMarkedAsSheet_overlaysTheEntriesBelow() {
        val word = entry(Word)
        val sentence = entry(Sentence, BottomSheetSceneStrategy.bottomSheet())

        val scene = with(strategy) { scope.calculateScene(listOf(word, sentence)) }

        val overlay = scene as OverlayScene<NavKey>
        assertThat(overlay.entries).containsExactly(sentence)
        assertThat(overlay.overlaidEntries).containsExactly(word)
        assertThat(overlay.previousEntries).containsExactly(word)
        assertThat(overlay.key).isEqualTo("Sentence")
    }

    @Test
    fun calculateScene_topEntryNotMarked_leavesItToTheNextStrategy() {
        val entries = listOf(entry(Sentence, BottomSheetSceneStrategy.bottomSheet()), entry(Word))

        assertThat(with(strategy) { scope.calculateScene(entries) }).isNull()
        assertThat(with(strategy) { scope.calculateScene(emptyList()) }).isNull()
    }
}
