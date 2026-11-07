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
package com.gyugle.gil.feature.curriculum.impl.data.repository

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.EntryId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LexiconEntry
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SearchHit
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SentenceBreakdown
import com.gyugle.gil.feature.curriculum.api.SentenceWord
import com.gyugle.gil.feature.curriculum.api.SoundEntry
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabaseProvider
import com.gyugle.gil.feature.curriculum.impl.data.dao.LexiconDao
import com.gyugle.gil.feature.curriculum.impl.data.dto.ChunkDto
import com.gyugle.gil.feature.curriculum.impl.data.dto.StepPayloadJson
import com.gyugle.gil.feature.curriculum.impl.data.dto.TokenDto
import com.gyugle.gil.feature.curriculum.impl.data.mapper.stage
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toCollocation
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toFamilyWord
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toGrammarNote
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toItemMeaning
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toLessonItem
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toSentence
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toSentenceChunk
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toSoundExample
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toStageTag
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toWordNote
import com.gyugle.gil.feature.curriculum.impl.data.mapper.toWordSense
import javax.inject.Inject

class RoomLexiconRepository @Inject constructor(private val databases: ContentDatabaseProvider) : LexiconObserver {

    override suspend fun getEntry(id: EntryId): Outcome<LexiconEntry, ContentError> = databases.read { database ->
        val dao = database.lexiconDao()
        val entry = when (id) {
            is WordId -> word(dao, id)
            is PatternId -> pattern(dao, id)
            is SoundId -> sound(dao, id)
        }
        entry?.let { Outcome.Success(it) } ?: Outcome.Failure(ContentError.NOT_FOUND)
    }

    override suspend fun getWordOf(id: SenseId): Outcome<WordId, ContentError> = databases.read { database ->
        database.lexiconDao().wordIdOf(id.value)
            ?.let { Outcome.Success(WordId(it)) }
            ?: Outcome.Failure(ContentError.NOT_FOUND)
    }

    override suspend fun getItems(ids: List<StudyItemId>): Outcome<List<LessonItem>, ContentError> =
        databases.read { database ->
            val dao = database.lexiconDao()
            // SQLite allows 999 bound values; the query binds the list three times.
            val rows = ids.map { it.value }.distinct().chunked(MAX_IDS_PER_QUERY)
                .flatMap { dao.items(it) }
                .associateBy { it.id }
            Outcome.Success(ids.mapNotNull { rows[it.value]?.toLessonItem() })
        }

    override suspend fun search(query: String, limit: Int): Outcome<List<SearchHit>, ContentError> =
        databases.read { database ->
            val dao = database.lexiconDao()
            val text = query.trim().replace('’', '\'')
            if (text.isEmpty() || limit <= 0) return@read Outcome.Success(emptyList())
            val korean = text.any { it.isHangul() }
            val key = if (korean) text else text.lowercase()
            val escaped = key.escapeLike()
            val words = if (korean) dao.wordsMeaning(escaped) else dao.wordsStartingWith(escaped)
            val patterns = if (korean) dao.patternsMeaning(escaped) else dao.patternsStartingWith(escaped)
            val stages = dao.stages().associate { it.id to it.toStageTag() }
            // A short query can match thousands of words; older SQLite takes 999 bound values.
            val senses = words.map { it.id }.chunked(MAX_IDS_PER_QUERY)
                .flatMap { dao.sensesOfWords(it) }
                .groupBy { it.wordId }
            val hits = words.map { word ->
                SearchHit(
                    id = WordId(word.id),
                    text = word.text,
                    ipa = word.ipa,
                    meanings = senses[word.id].orEmpty().map { it.toItemMeaning(stages) }
                )
            } + patterns.map { pattern ->
                SearchHit(
                    id = PatternId(pattern.id),
                    text = pattern.text,
                    ipa = null,
                    meanings = listOf(pattern.toItemMeaning(stages))
                )
            }
            Outcome.Success(hits.sortedWith(searchOrder(key)).take(limit))
        }

    override suspend fun getSentence(id: Long): Outcome<SentenceBreakdown, ContentError> = databases.read { database ->
        val dao = database.lexiconDao()
        val sentence = dao.sentence(id) ?: return@read Outcome.Failure(ContentError.NOT_FOUND)
        val tokens = StepPayloadJson.decodeFromString<List<TokenDto>>(sentence.tokens).filter { it.word != null }
        val chunks = sentence.chunks?.let { StepPayloadJson.decodeFromString<List<ChunkDto>>(it) }.orEmpty()
        // The screen marks words and chunks by these offsets, so a broken range is broken content.
        (tokens.map { it.start to it.end } + chunks.map { it.start to it.end }).forEach { (start, end) ->
            check(start in 0 until end && end <= sentence.text.length) { "Sentence $id has a range $start..$end" }
        }
        val headwords = dao.words(tokens.mapNotNull { it.word }.distinct()).associate { it.id to it.text }
        val stages = dao.stages().associate { it.id to it.toStageTag() }
        val meanings = dao.senses(tokens.mapNotNull { it.sense }.distinct())
            .associate { it.id to it.toItemMeaning(stages) }
        Outcome.Success(
            SentenceBreakdown(
                sentence = Sentence(id = sentence.id, text = sentence.text, ko = sentence.translationKo),
                words = tokens.map { token ->
                    val wordId = checkNotNull(token.word)
                    SentenceWord(
                        start = token.start,
                        end = token.end,
                        wordId = WordId(wordId),
                        headword = checkNotNull(headwords[wordId]) { "Sentence $id links a missing word $wordId" },
                        sense = token.sense?.let { checkNotNull(meanings[it]) { "Sentence $id links a missing $it" } }
                    )
                },
                chunks = chunks.map { it.toSentenceChunk() },
                literalKo = sentence.literalKo,
                grammar = sentence.grammarId?.let { grammarId ->
                    checkNotNull(dao.grammar(grammarId)) { "Sentence $id links a missing $grammarId" }.toGrammarNote()
                }
            )
        )
    }

    private suspend fun word(dao: LexiconDao, id: WordId): WordEntry? {
        val word = dao.word(id.value) ?: return null
        val stages = dao.stages().associate { it.id to it.toStageTag() }
        val senses = dao.sensesOfWords(listOf(word.id))
        val examples = dao.senseExamples(senses.map { it.id }).groupBy { it.ownerId }
        return WordEntry(
            id = id,
            text = word.text,
            ipa = word.ipa,
            hangul = word.hangul,
            senses = senses.map { it.toWordSense(stages, examples[it.id].orEmpty()) },
            collocations = dao.collocations(word.id).map { it.toCollocation() },
            family = dao.family(word.id).map { it.toFamilyWord() },
            notes = dao.notes(word.id).map { it.toWordNote() }
        )
    }

    private suspend fun pattern(dao: LexiconDao, id: PatternId): PatternEntry? {
        val pattern = dao.pattern(id.value) ?: return null
        val stages = dao.stages().associate { it.id to it.toStageTag() }
        return PatternEntry(
            id = id,
            text = pattern.text,
            meaningKo = pattern.meaningKo,
            noteKo = pattern.noteKo,
            stage = stages.stage(pattern.stageId),
            examples = dao.patternExamples(pattern.id).map { it.toSentence() }
        )
    }

    private suspend fun sound(dao: LexiconDao, id: SoundId): SoundEntry? {
        val sound = dao.sound(id.value) ?: return null
        val stages = dao.stages().associate { it.id to it.toStageTag() }
        return SoundEntry(
            id = id,
            symbol = sound.symbol,
            spelling = sound.spelling,
            descriptionKo = sound.descriptionKo,
            stage = stages.stage(sound.stageId),
            examples = dao.soundExamples(sound.id).map { it.toSoundExample() }
        )
    }

    private companion object {
        const val MAX_IDS_PER_QUERY = 300
    }
}

/**
 * The headword itself first, then headwords starting with [key], then the rest; within each, the
 * earliest stage of any meaning, then alphabetical.
 */
private fun searchOrder(key: String): Comparator<SearchHit> = compareBy<SearchHit>(
    { hit ->
        val text = hit.text.lowercase().replace('’', '\'').trimEnd('.', '?', '!')
        when {
            text == key -> 0
            text.startsWith(key) -> 1
            else -> 2
        }
    },
    { hit -> hit.meanings.minOfOrNull { it.stage.position } ?: Int.MAX_VALUE },
    { hit -> hit.text.lowercase() }
)

private fun String.escapeLike(): String = buildString {
    this@escapeLike.forEach { char ->
        if (char == '%' || char == '_' || char == '\\') append('\\')
        append(char)
    }
}

/** Hangul syllables and jamo, so a half-typed "ㅇ" also searches meanings. */
private fun Char.isHangul(): Boolean = this in '가'..'힣' || this in 'ㄱ'..'ㆎ'
