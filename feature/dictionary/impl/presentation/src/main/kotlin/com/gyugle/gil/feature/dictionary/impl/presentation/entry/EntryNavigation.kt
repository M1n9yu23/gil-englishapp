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

import androidx.compose.runtime.Immutable

/**
 * Where a page can lead: back, to another entry, or to an example's breakdown. The breakdown gets
 * the ID of the page's entry, so it can tell when the learner asks for the page they came from.
 */
@Immutable
internal class EntryNavigation(
    val onBack: () -> Unit,
    val onOpenEntry: (String) -> Unit,
    val onOpenSentence: (sentenceId: Long, entryId: String) -> Unit
)
