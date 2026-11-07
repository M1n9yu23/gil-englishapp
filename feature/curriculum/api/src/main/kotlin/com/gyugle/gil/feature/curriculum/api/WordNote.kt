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

/** A note on a word in Korean. */
data class WordNote(val kind: Kind, val ko: String) {
    enum class Kind {
        /** English that Korean uses differently, such as "fighting". */
        KONGLISH,

        /** A mistake Korean learners often make. */
        MISTAKE,

        /** How the word is used. */
        USAGE
    }
}
