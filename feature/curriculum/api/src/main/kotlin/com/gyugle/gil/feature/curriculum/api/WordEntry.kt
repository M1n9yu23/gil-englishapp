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
 * A word and everything the dictionary says about it. [ipa] has no slashes; [hangul] is the Hangul
 * pronunciation aid, given only for words of the first stages. [senses] come in the order the
 * content lists them.
 */
data class WordEntry(
    override val id: WordId,
    val text: String,
    val ipa: String,
    val hangul: String?,
    val senses: List<WordSense>,
    val collocations: List<Collocation>,
    val family: List<FamilyWord>,
    val notes: List<WordNote>
) : LexiconEntry
