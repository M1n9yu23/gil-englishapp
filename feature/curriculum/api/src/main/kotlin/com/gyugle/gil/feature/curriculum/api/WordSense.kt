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
package com.gyugle.gil.feature.curriculum.api

/**
 * One meaning of a word. [ipa] is set only when this sense is said differently from the word.
 * [stage] is where the course teaches it, or the level it belongs to when no lesson does yet.
 */
data class WordSense(
    val id: SenseId,
    val partOfSpeech: PartOfSpeech,
    val meaningKo: String,
    val definitionEn: String?,
    val ipa: String?,
    val stage: StageTag,
    val examples: List<Sentence>
)
