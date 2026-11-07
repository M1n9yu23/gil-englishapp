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
package com.gyugle.gil.feature.curriculum.impl.data.mapper

import com.gyugle.gil.feature.curriculum.api.ChunkRole
import com.gyugle.gil.feature.curriculum.api.Collocation
import com.gyugle.gil.feature.curriculum.api.FamilyWord
import com.gyugle.gil.feature.curriculum.api.GrammarNote
import com.gyugle.gil.feature.curriculum.api.ItemMeaning
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SentenceChunk
import com.gyugle.gil.feature.curriculum.api.SoundExample
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordNote
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.curriculum.impl.data.dao.ExampleRow
import com.gyugle.gil.feature.curriculum.impl.data.dao.ItemRow
import com.gyugle.gil.feature.curriculum.impl.data.dto.ChunkDto
import com.gyugle.gil.feature.curriculum.impl.data.entity.GrammarEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.PatternEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SenseEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SoundExampleEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordCollocationEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordFamilyEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordNoteEntity

// The compiler checks parts of speech, note kinds, roles and stages, so an unknown one means a
// broken file; the repository reports it as unavailable content.

fun StageEntity.toStageTag(): StageTag = StageTag(id = StageId(id), position = position, title = localizedTitle())

fun Map<String, StageTag>.stage(id: String): StageTag = checkNotNull(get(id)) { "No stage $id" }

fun SenseEntity.toWordSense(stages: Map<String, StageTag>, examples: List<ExampleRow>): WordSense = WordSense(
    id = SenseId(id),
    partOfSpeech = partOfSpeech(pos),
    meaningKo = meaningKo,
    definitionEn = definitionEn,
    ipa = ipa,
    stage = stages.stage(stageId),
    examples = examples.map { it.toSentence() }
)

fun SenseEntity.toItemMeaning(stages: Map<String, StageTag>): ItemMeaning = ItemMeaning(
    id = SenseId(id),
    partOfSpeech = partOfSpeech(pos),
    meaningKo = meaningKo,
    stage = stages.stage(stageId)
)

fun PatternEntity.toItemMeaning(stages: Map<String, StageTag>): ItemMeaning =
    ItemMeaning(id = PatternId(id), partOfSpeech = null, meaningKo = meaningKo, stage = stages.stage(stageId))

fun ExampleRow.toSentence(): Sentence = Sentence(id = id, text = text, ko = translationKo)

fun WordCollocationEntity.toCollocation(): Collocation = Collocation(text = textEn, ko = textKo)

fun WordFamilyEntity.toFamilyWord(): FamilyWord = FamilyWord(text = text, id = relatedWordId?.let(::WordId))

fun WordNoteEntity.toWordNote(): WordNote = WordNote(
    kind = when (kind) {
        "konglish" -> WordNote.Kind.KONGLISH
        "mistake" -> WordNote.Kind.MISTAKE
        "usage" -> WordNote.Kind.USAGE
        else -> error("Unknown note kind $kind")
    },
    ko = textKo
)

fun SoundExampleEntity.toSoundExample(): SoundExample = SoundExample(text = text, ipa = ipa)

fun ItemRow.toLessonItem(): LessonItem =
    LessonItem(id = id.toStudyItemId(), text = text, ipa = ipa, meaningKo = meaningKo)

fun ChunkDto.toSentenceChunk(): SentenceChunk =
    SentenceChunk(start = start, end = end, role = checkNotNull(ChunkRole.fromKey(role)) { "Unknown role $role" })

fun GrammarEntity.toGrammarNote(): GrammarNote = GrammarNote(
    id = id,
    titleKo = titleKo,
    paragraphs = bodyKo.split(BLANK_LINE).map { it.trim() }.filter { it.isNotEmpty() }
)

private val BLANK_LINE = Regex("""\n\s*\n""")

private fun partOfSpeech(key: String): PartOfSpeech =
    checkNotNull(PartOfSpeech.fromKey(key)) { "Unknown part of speech $key" }
