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
package com.gyugle.gil.feature.dictionary.impl.presentation

import com.gyugle.gil.feature.curriculum.api.ItemMeaning
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SearchHit
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SoundEntry
import com.gyugle.gil.feature.curriculum.api.SoundExample
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordSense

/** Dictionary content for the tests: "run" with an early and a late sense, a pattern and a sound. */
internal object TestEntries {
    val firstSteps = StageTag(StageId("stage.a1"), 2, LocalizedText("첫걸음", "First steps"))
    val onYourOwn = StageTag(StageId("stage.b1"), 4, LocalizedText("자립", "On your own"))

    val moveFast = WordSense(
        id = SenseId("sense.run.v.move-fast"),
        partOfSpeech = PartOfSpeech.VERB,
        meaningKo = "달리다",
        definitionEn = null,
        ipa = null,
        stage = firstSteps,
        examples = listOf(Sentence(1, "I run.", "나는 달려요."))
    )
    val manage = WordSense(
        id = SenseId("sense.run.v.manage"),
        partOfSpeech = PartOfSpeech.VERB,
        meaningKo = "운영하다",
        definitionEn = null,
        ipa = null,
        stage = onYourOwn,
        examples = listOf(Sentence(2, "She runs a shop.", "그녀는 가게를 운영해요."))
    )
    val run = WordEntry(
        id = WordId("word.run"),
        text = "run",
        ipa = "rʌn",
        hangul = null,
        senses = listOf(moveFast, manage),
        collocations = emptyList(),
        family = emptyList(),
        notes = emptyList()
    )
    val thankYou = PatternEntry(
        id = PatternId("pattern.thank-you"),
        text = "Thank you.",
        meaningKo = "고마워요",
        noteKo = null,
        stage = firstSteps,
        examples = emptyList()
    )
    val f = SoundEntry(
        id = SoundId("sound.f"),
        symbol = "f",
        spelling = "f, ph",
        descriptionKo = "윗니를 아랫입술에 대요.",
        stage = firstSteps,
        examples = listOf(SoundExample("fan", "fæn"))
    )

    fun hit(entry: WordEntry) = SearchHit(
        id = entry.id,
        text = entry.text,
        ipa = entry.ipa,
        meanings = entry.senses.map { ItemMeaning(it.id, it.partOfSpeech, it.meaningKo, it.stage) }
    )

    fun hit(entry: PatternEntry) = SearchHit(
        id = entry.id,
        text = entry.text,
        ipa = null,
        meanings = listOf(ItemMeaning(entry.id, null, entry.meaningKo, entry.stage))
    )
}
