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

/** A dictionary page. [id] is its content ID and [headline] the text a content report quotes. */
@Immutable
sealed interface EntryUi {
    val id: String
    val headline: String

    /**
     * [learnedAny] is false when the learner has met none of the senses, and [now] holds the ones
     * the course teaches first.
     */
    data class Word(
        override val id: String,
        val text: String,
        val ipa: String,
        val hangul: String?,
        val learnedAny: Boolean,
        val now: List<SenseUi>,
        val later: List<LaterSenseUi>,
        val collocations: List<PhraseUi>,
        val family: List<FamilyWordUi>,
        val notes: List<NoteUi>
    ) : EntryUi {
        override val headline: String get() = text
    }

    data class Pattern(
        override val id: String,
        val text: String,
        val meaningKo: String,
        val noteKo: String?,
        val examples: List<ExampleUi>,
        val saved: Boolean
    ) : EntryUi {
        override val headline: String get() = text
    }

    /** [symbol] is IPA without slashes, [spelling] the letters that usually spell it. */
    data class Sound(
        override val id: String,
        val symbol: String,
        val spelling: String,
        val descriptionKo: String,
        val examples: List<SoundWordUi>,
        val saved: Boolean
    ) : EntryUi {
        override val headline: String get() = spelling
    }
}
