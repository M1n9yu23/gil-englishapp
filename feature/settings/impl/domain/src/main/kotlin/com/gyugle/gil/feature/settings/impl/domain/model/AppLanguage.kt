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
package com.gyugle.gil.feature.settings.impl.domain.model

/** The language of the app's screens. [tag] is the BCP 47 language tag; [SYSTEM] has none. */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    KOREAN("ko"),
    ENGLISH("en");

    companion object {
        /**
         * The language chosen by comma-separated [tags], as the system reports them. Only the
         * first tag counts, and its region is ignored. A language the app does not have means the
         * app follows the phone.
         */
        fun fromTags(tags: String): AppLanguage {
            val language = tags.substringBefore(',').substringBefore('-').trim()
            return entries.firstOrNull { it.tag != null && it.tag.equals(language, ignoreCase = true) } ?: SYSTEM
        }
    }
}
