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
package com.gyugle.gil.feature.dictionary.impl.presentation.entry

sealed interface EntryAction {
    data object OnRetryClick : EntryAction

    /** Plays [text]: the headword in either accent, or an example in the American one. */
    data class OnPlayClick(val text: String, val accent: Accent = Accent.AMERICAN) : EntryAction

    /** Adds the study item [itemId] to the word list, or takes it out. */
    data class OnSaveToggle(val itemId: String) : EntryAction
}
