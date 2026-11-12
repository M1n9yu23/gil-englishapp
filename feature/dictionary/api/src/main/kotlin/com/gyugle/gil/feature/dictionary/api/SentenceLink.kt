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
package com.gyugle.gil.feature.dictionary.api

/**
 * A data URI that opens the breakdown of sentence [sentenceId] over the dictionary page of
 * [entryId], the word, pattern or sound it is an example of. Put [uri] on an explicit intent to the
 * app's launch activity.
 */
data class SentenceLink(val sentenceId: Long, val entryId: String) {
    val uri: String get() = "$PREFIX$sentenceId/$entryId"

    companion object {
        private const val PREFIX = "gil://sentence/"

        /** A positive id in ASCII digits, then a content ID such as `word.run` or `pattern.how-are-you`. */
        private val PATH = Regex("([1-9][0-9]{0,17})/([a-z]+\\.[a-z0-9.-]+)")

        /**
         * Reads a link made by [uri]. Returns null for anything else, including an ID with
         * characters content IDs never have. Whether the content has the entry is not checked here.
         */
        fun parse(uri: String): SentenceLink? {
            if (!uri.startsWith(PREFIX)) return null
            val match = PATH.matchEntire(uri.substring(PREFIX.length)) ?: return null
            val (sentenceId, entryId) = match.destructured
            return SentenceLink(sentenceId.toLong(), entryId)
        }
    }
}
