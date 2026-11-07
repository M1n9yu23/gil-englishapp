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
package com.gyugle.gil.feature.dictionary.impl.presentation.sentence

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * A word the learner can tap. [partOfSpeech] and [meaningKo] are its sense in this sentence, null
 * when the content does not say which sense it is.
 */
@Immutable
data class SentenceWordUi(
    val start: Int,
    val end: Int,
    val wordId: String,
    val headword: String,
    @param:StringRes val partOfSpeech: Int?,
    val meaningKo: String?
)
