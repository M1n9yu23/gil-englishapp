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
 * A word of a sentence that the dictionary has. A headword of several words, such as "thank you",
 * is one [SentenceWord] per word, all with the same [wordId]. [sense] is the meaning it has here,
 * or null when the content does not say which of the word's senses it is.
 */
data class SentenceWord(val start: Int, val end: Int, val wordId: WordId, val headword: String, val sense: ItemMeaning?)
