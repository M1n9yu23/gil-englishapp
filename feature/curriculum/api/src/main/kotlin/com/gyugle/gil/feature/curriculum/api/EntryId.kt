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
 * Something the dictionary has a page for: a word, a pattern or a sound. A sense has no page of
 * its own; it is shown on its word's page.
 */
sealed interface EntryId {
    val value: String

    companion object {
        /** Returns null for an ID that has no page, such as a sense or a lesson. */
        fun parse(value: String): EntryId? = when {
            WordId.isWordId(value) -> WordId(value)

            else -> when (val item = StudyItemId.parse(value)) {
                is PatternId -> item
                is SoundId -> item
                is SenseId, null -> null
            }
        }
    }
}
