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
package com.gyugle.gil.feature.dictionary.impl.domain

import com.gyugle.gil.feature.curriculum.api.ItemMeaning
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.SearchHit
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordSense

internal fun stage(position: Int) =
    StageTag(StageId("stage.$position"), position, LocalizedText("$position", "$position"))

internal fun sense(name: String, stage: Int) = WordSense(
    id = SenseId("sense.$name"),
    partOfSpeech = PartOfSpeech.VERB,
    meaningKo = name,
    definitionEn = null,
    ipa = null,
    stage = stage(stage),
    examples = emptyList()
)

internal fun hit(word: String, vararg senses: String) = SearchHit(
    id = WordId("word.$word"),
    text = word,
    ipa = null,
    meanings = senses.map { ItemMeaning(SenseId("sense.$it"), PartOfSpeech.VERB, it, stage(1)) }
)
