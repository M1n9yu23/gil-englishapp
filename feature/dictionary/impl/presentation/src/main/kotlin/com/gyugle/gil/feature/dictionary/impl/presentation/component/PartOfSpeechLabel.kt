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
package com.gyugle.gil.feature.dictionary.impl.presentation.component

import androidx.annotation.StringRes
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.dictionary.impl.presentation.R

@StringRes
internal fun PartOfSpeech.label(): Int = when (this) {
    PartOfSpeech.NOUN -> R.string.dictionary_pos_noun
    PartOfSpeech.VERB -> R.string.dictionary_pos_verb
    PartOfSpeech.ADJECTIVE -> R.string.dictionary_pos_adjective
    PartOfSpeech.ADVERB -> R.string.dictionary_pos_adverb
    PartOfSpeech.PREPOSITION -> R.string.dictionary_pos_preposition
    PartOfSpeech.PRONOUN -> R.string.dictionary_pos_pronoun
    PartOfSpeech.CONJUNCTION -> R.string.dictionary_pos_conjunction
    PartOfSpeech.INTERJECTION -> R.string.dictionary_pos_interjection
    PartOfSpeech.MODAL -> R.string.dictionary_pos_modal
    PartOfSpeech.ARTICLE -> R.string.dictionary_pos_article
    PartOfSpeech.DETERMINER -> R.string.dictionary_pos_determiner
    PartOfSpeech.NUMBER -> R.string.dictionary_pos_number
}
