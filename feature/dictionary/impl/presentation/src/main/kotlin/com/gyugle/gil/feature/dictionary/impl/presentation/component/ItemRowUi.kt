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
package com.gyugle.gil.feature.dictionary.impl.presentation.component

import androidx.compose.runtime.Immutable

/**
 * One line of a word list. [id] is what the entry page opens. [stage], on a word the learner has
 * not met, names the stage that teaches it; [mistakes] is how often the learner got it wrong. A
 * sound has no meaning, so its row says it is a sound instead.
 */
@Immutable
data class ItemRowUi(
    val id: String,
    val text: String,
    val ipa: String?,
    val meaningKo: String?,
    val stage: LocalizedTextUi? = null,
    val mistakes: Int? = null,
    val isSound: Boolean = false
)
