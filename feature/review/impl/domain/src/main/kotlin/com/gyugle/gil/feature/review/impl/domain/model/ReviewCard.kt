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
package com.gyugle.gil.feature.review.impl.domain.model

import com.gyugle.gil.feature.curriculum.api.SoundExample
import com.gyugle.gil.feature.curriculum.api.StudyItemId

/** One question about one study item. */
sealed interface ReviewCard {
    val itemId: StudyItemId

    /** A card answered by picking: its options are in display order, [answer] is the right one. */
    sealed interface ChoiceCard : ReviewCard {
        val optionCount: Int
        val answer: Int
    }

    /** English, seen and also heard when [speakable], to its Korean meaning. */
    data class ChooseMeaning(
        override val itemId: StudyItemId,
        val english: String,
        val ipa: String?,
        val speakable: Boolean,
        val options: List<String>,
        override val answer: Int
    ) : ChoiceCard {
        override val optionCount: Int get() = options.size
    }

    /** A Korean meaning to the English it means. */
    data class ChooseEnglish(
        override val itemId: StudyItemId,
        val meaningKo: String,
        val options: List<String>,
        override val answer: Int
    ) : ChoiceCard {
        override val optionCount: Int get() = options.size
    }

    /** English only heard, to which English it was. [meaningKo] shows once answered. */
    data class ListenChoose(
        override val itemId: StudyItemId,
        val spoken: String,
        val meaningKo: String?,
        val options: List<String>,
        override val answer: Int
    ) : ChoiceCard {
        override val optionCount: Int get() = options.size
    }

    /** A letter's name, heard as [spoken] or read as [nameIpa], to the letter. */
    data class ChooseLetter(
        override val itemId: StudyItemId,
        val spoken: String,
        val nameIpa: String,
        val options: List<String>,
        override val answer: Int
    ) : ChoiceCard {
        override val optionCount: Int get() = options.size
    }

    /** A sound's symbol to the word that has the sound. [descriptionKo] explains it once answered. */
    data class FindSound(
        override val itemId: StudyItemId,
        val symbol: String,
        val descriptionKo: String,
        val options: List<SoundExample>,
        override val answer: Int
    ) : ChoiceCard {
        override val optionCount: Int get() = options.size
    }

    /** A word, heard when [speakable], with its meaning and IPA, to its spelling. */
    data class Spell(
        override val itemId: StudyItemId,
        val word: String,
        val meaningKo: String,
        val ipa: String?,
        val speakable: Boolean
    ) : ReviewCard

    /**
     * A sentence with the item left out, from [blankStart] to [blankEnd], to the word as the
     * sentence has it.
     */
    data class FillBlank(
        override val itemId: StudyItemId,
        val sentence: String,
        val blankStart: Int,
        val blankEnd: Int,
        val translation: String?,
        val meaningKo: String
    ) : ReviewCard {
        val answer: String get() = sentence.substring(blankStart, blankEnd)
    }
}
