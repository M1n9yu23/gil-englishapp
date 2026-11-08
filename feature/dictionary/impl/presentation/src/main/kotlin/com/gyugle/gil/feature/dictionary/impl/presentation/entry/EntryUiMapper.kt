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

import com.gyugle.gil.feature.curriculum.api.LexiconEntry
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SoundEntry
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordNote
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.dictionary.impl.domain.groupSenses
import com.gyugle.gil.feature.dictionary.impl.presentation.R
import com.gyugle.gil.feature.dictionary.impl.presentation.component.label
import com.gyugle.gil.feature.dictionary.impl.presentation.component.toLocalizedTextUi

/**
 * Builds the page from the entry and the learner's record: [learned] items have cards, [saved]
 * ones are in the word list. [focus] is the sense a list opened the page for. The Hangul aid shows
 * only with [showHangulAid], the learner's setting.
 */
internal fun LexiconEntry.toEntryUi(
    learned: Set<StudyItemId>,
    saved: Set<StudyItemId>,
    focus: SenseId? = null,
    showHangulAid: Boolean = true
): EntryUi = when (this) {
    is WordEntry -> {
        val groups = groupSenses(senses, learned, focus)
        EntryUi.Word(
            id = id.value,
            text = text,
            ipa = ipa,
            hangul = hangul?.takeIf { showHangulAid },
            learnedAny = groups.learnedAny,
            now = groups.now.map { it.toSenseUi(saved = it.id in saved) },
            later = groups.later.map {
                LaterSenseUi(
                    id = it.id.value,
                    partOfSpeech = it.partOfSpeech.label(),
                    meaningKo = it.meaningKo,
                    stage = it.stage.title.toLocalizedTextUi()
                )
            },
            collocations = collocations.map { PhraseUi(text = it.text, ko = it.ko) },
            family = family.map { FamilyWordUi(text = it.text, entryId = it.id?.value) },
            notes = notes.map { NoteUi(label = it.kind.label(), ko = it.ko) }
        )
    }

    is PatternEntry -> EntryUi.Pattern(
        id = id.value,
        text = text,
        meaningKo = meaningKo,
        noteKo = noteKo,
        examples = examples.map { it.toExampleUi() },
        saved = id in saved
    )

    is SoundEntry -> EntryUi.Sound(
        id = id.value,
        symbol = symbol,
        spelling = spelling,
        descriptionKo = descriptionKo,
        examples = examples.map { SoundWordUi(text = it.text, ipa = it.ipa) },
        saved = id in saved
    )
}

private fun WordSense.toSenseUi(saved: Boolean) = SenseUi(
    id = id.value,
    partOfSpeech = partOfSpeech.label(),
    meaningKo = meaningKo,
    definitionEn = definitionEn,
    ipa = ipa,
    examples = examples.map { it.toExampleUi() },
    saved = saved
)

private fun Sentence.toExampleUi() = ExampleUi(sentenceId = id, text = text, ko = ko)

private fun WordNote.Kind.label(): Int = when (this) {
    WordNote.Kind.KONGLISH -> R.string.dictionary_note_konglish
    WordNote.Kind.MISTAKE -> R.string.dictionary_note_mistake
    WordNote.Kind.USAGE -> R.string.dictionary_note_usage
}
