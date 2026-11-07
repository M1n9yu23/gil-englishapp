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
package com.gyugle.gil.feature.curriculum.impl.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.gyugle.gil.feature.curriculum.impl.data.entity.GrammarEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.PatternEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SenseEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SentenceEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SoundEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.SoundExampleEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.StageEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordCollocationEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordFamilyEntity
import com.gyugle.gil.feature.curriculum.impl.data.entity.WordNoteEntity

/**
 * Reads dictionary entries, search matches and sentences. Lists come in the order the content
 * gives. Search patterns are LIKE patterns that escape `%` and `_` with a backslash.
 */
@Dao
interface LexiconDao {
    @Query("SELECT * FROM stage")
    suspend fun stages(): List<StageEntity>

    @Query("SELECT * FROM word WHERE id = :id")
    suspend fun word(id: String): WordEntity?

    @Query("SELECT * FROM word WHERE id IN (:ids)")
    suspend fun words(ids: List<String>): List<WordEntity>

    @Query("SELECT * FROM sense WHERE wordId IN (:wordIds) ORDER BY wordId, position")
    suspend fun sensesOfWords(wordIds: List<String>): List<SenseEntity>

    @Query("SELECT * FROM sense WHERE id IN (:ids)")
    suspend fun senses(ids: List<String>): List<SenseEntity>

    @Query("SELECT wordId FROM sense WHERE id = :senseId")
    suspend fun wordIdOf(senseId: String): String?

    @Query(
        """
        SELECT e.senseId AS ownerId, s.id AS id, s.text AS text, s.translationKo AS translationKo
        FROM sense_example e JOIN sentence s ON s.id = e.sentenceId
        WHERE e.senseId IN (:senseIds) ORDER BY e.senseId, e.position
        """
    )
    suspend fun senseExamples(senseIds: List<String>): List<ExampleRow>

    @Query("SELECT * FROM word_collocation WHERE wordId = :wordId ORDER BY position")
    suspend fun collocations(wordId: String): List<WordCollocationEntity>

    @Query("SELECT * FROM word_family WHERE wordId = :wordId ORDER BY position")
    suspend fun family(wordId: String): List<WordFamilyEntity>

    @Query("SELECT * FROM word_note WHERE wordId = :wordId ORDER BY position")
    suspend fun notes(wordId: String): List<WordNoteEntity>

    @Query("SELECT * FROM pattern WHERE id = :id")
    suspend fun pattern(id: String): PatternEntity?

    @Query(
        """
        SELECT e.patternId AS ownerId, s.id AS id, s.text AS text, s.translationKo AS translationKo
        FROM pattern_example e JOIN sentence s ON s.id = e.sentenceId
        WHERE e.patternId = :patternId ORDER BY e.position
        """
    )
    suspend fun patternExamples(patternId: String): List<ExampleRow>

    @Query("SELECT * FROM sound WHERE id = :id")
    suspend fun sound(id: String): SoundEntity?

    @Query("SELECT * FROM sound_example WHERE soundId = :soundId ORDER BY position")
    suspend fun soundExamples(soundId: String): List<SoundExampleEntity>

    /** A line for each of [ids] that exists, in no particular order. */
    @Query(
        """
        SELECT s.id AS id, w.text AS text, COALESCE(s.ipa, w.ipa) AS ipa, s.meaningKo AS meaningKo
        FROM sense s JOIN word w ON w.id = s.wordId WHERE s.id IN (:ids)
        UNION ALL
        SELECT id, text, NULL, meaningKo FROM pattern WHERE id IN (:ids)
        UNION ALL
        SELECT id, spelling, symbol, NULL FROM sound WHERE id IN (:ids)
        """
    )
    suspend fun items(ids: List<String>): List<ItemRow>

    /**
     * Words with a headword or form starting with [prefix]: lowercase, with a straight apostrophe,
     * LIKE-escaped. The compiler stores the search key in the same form.
     */
    @Query("""SELECT * FROM word WHERE searchKey LIKE '% ' || :prefix || '%' ESCAPE '\'""")
    suspend fun wordsStartingWith(prefix: String): List<WordEntity>

    /** Words with a meaning that contains [text], LIKE-escaped. */
    @Query(
        """
        SELECT * FROM word WHERE id IN
            (SELECT wordId FROM sense WHERE meaningKo LIKE '%' || :text || '%' ESCAPE '\')
        """
    )
    suspend fun wordsMeaning(text: String): List<WordEntity>

    /** Patterns with a word starting with [prefix], given as for [wordsStartingWith]. */
    @Query(
        """
        SELECT * FROM pattern WHERE ' ' || replace(lower(text), '’', '''') LIKE '% ' || :prefix || '%' ESCAPE '\'
        """
    )
    suspend fun patternsStartingWith(prefix: String): List<PatternEntity>

    /** Patterns with a meaning that contains [text], LIKE-escaped. */
    @Query("""SELECT * FROM pattern WHERE meaningKo LIKE '%' || :text || '%' ESCAPE '\'""")
    suspend fun patternsMeaning(text: String): List<PatternEntity>

    @Query("SELECT * FROM sentence WHERE id = :id")
    suspend fun sentence(id: Long): SentenceEntity?

    @Query("SELECT * FROM grammar WHERE id = :id")
    suspend fun grammar(id: String): GrammarEntity?
}
