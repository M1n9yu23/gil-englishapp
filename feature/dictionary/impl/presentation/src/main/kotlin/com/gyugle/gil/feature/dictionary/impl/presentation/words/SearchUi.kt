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
package com.gyugle.gil.feature.dictionary.impl.presentation.words

import androidx.compose.runtime.Immutable
import com.gyugle.gil.feature.dictionary.impl.presentation.ContentErrorUi
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ItemRowUi

/** What the search list shows. Results stay up while the next search runs. */
@Immutable
sealed interface SearchUi {
    /** Nothing typed yet. */
    data object Idle : SearchUi

    data class Results(val learned: List<ItemRowUi>, val notLearned: List<ItemRowUi>) : SearchUi

    data class NoResults(val query: String) : SearchUi

    data class Failed(val error: ContentErrorUi) : SearchUi
}
