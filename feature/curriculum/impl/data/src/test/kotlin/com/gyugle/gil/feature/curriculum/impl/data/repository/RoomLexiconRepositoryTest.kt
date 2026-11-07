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

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ChunkRole
import com.gyugle.gil.feature.curriculum.api.Collocation
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FamilyWord
import com.gyugle.gil.feature.curriculum.api.GrammarNote
import com.gyugle.gil.feature.curriculum.api.ItemMeaning
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SearchHit
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SentenceChunk
import com.gyugle.gil.feature.curriculum.api.SentenceWord
import com.gyugle.gil.feature.curriculum.api.SoundEntry
import com.gyugle.gil.feature.curriculum.api.SoundExample
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordNote
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabase
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Runs the real dictionary queries on an in-memory copy of the schema, rows out of order. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomLexiconRepositoryTest {
    private val database = Room.inMemoryDatabaseBuilder(
        RuntimeEnvironment.getApplication(),
        ContentDatabase::class.java
    )
        .build()
        .apply { openHelper.writableDatabase.fill() }
    private val repository = RoomLexiconRepository { database }

    private val one = StageTag(StageId("stage.one"), 1, LocalizedText(ko = "하나", en = "One"))
    private val two = StageTag(StageId("stage.two"), 2, LocalizedText(ko = "둘", en = "Two"))

    @After
    fun close() = database.close()

    @Test
    fun getEntry_word_returnsSensesInOrderWithExamplesAndNotes() = runTest {
        val entry = repository.getEntry(WordId("word.run"))

        assertThat(entry).isEqualTo(
            Outcome.Success(
                WordEntry(
                    id = WordId("word.run"),
                    text = "run",
                    ipa = "rʌn",
                    hangul = "런",
                    senses = listOf(
                        WordSense(
                            id = SenseId("sense.run.v.move-fast"),
                            partOfSpeech = PartOfSpeech.VERB,
                            meaningKo = "달리다",
                            definitionEn = null,
                            ipa = null,
                            stage = one,
                            examples = listOf(
                                Sentence(2, "I run.", "나는 달려요."),
                                Sentence(1, "We run every day.", "우리는 매일 달려요.")
                            )
                        ),
                        WordSense(
                            id = SenseId("sense.run.v.manage"),
                            partOfSpeech = PartOfSpeech.VERB,
                            meaningKo = "운영하다",
                            definitionEn = "to be in charge of",
                            ipa = null,
                            stage = two,
                            examples = emptyList()
                        )
                    ),
                    collocations = listOf(Collocation("run a shop", "가게를 운영하다")),
                    family = listOf(FamilyWord("runner", WordId("word.runner")), FamilyWord("running", null)),
                    notes = listOf(
                        WordNote(WordNote.Kind.MISTAKE, "`run`의 과거형은 `ran`이에요."),
                        WordNote(WordNote.Kind.KONGLISH, "`running machine`은 콩글리시예요.")
                    )
                )
            )
        )
    }

    @Test
    fun getEntry_pattern_returnsMeaningNoteAndExamples() = runTest {
        assertThat(repository.getEntry(PatternId("pattern.thank-you"))).isEqualTo(
            Outcome.Success(
                PatternEntry(
                    id = PatternId("pattern.thank-you"),
                    text = "Thank you.",
                    meaningKo = "고마워요",
                    noteKo = "`Thanks.`보다 조금 더 정중해요.",
                    stage = one,
                    examples = listOf(Sentence(3, "Thank you, Sam.", "고마워요, 샘."))
                )
            )
        )
    }

    @Test
    fun getEntry_sound_returnsExampleWordsInOrder() = runTest {
        assertThat(repository.getEntry(SoundId("sound.f"))).isEqualTo(
            Outcome.Success(
                SoundEntry(
                    id = SoundId("sound.f"),
                    symbol = "f",
                    spelling = "f, ph",
                    descriptionKo = "윗니를 아랫입술에 대요.",
                    stage = one,
                    examples = listOf(SoundExample("fan", "fæn"), SoundExample("phone", "foʊn"))
                )
            )
        )
    }

    @Test
    fun getEntry_unknownIds_notFound() = runTest {
        assertThat(repository.getEntry(WordId("word.walk"))).isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
        assertThat(repository.getEntry(PatternId("pattern.walk"))).isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
        assertThat(repository.getEntry(SoundId("sound.walk"))).isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
    }

    @Test
    fun getWordOf_knownAndUnknownSense() = runTest {
        assertThat(repository.getWordOf(SenseId("sense.run.v.manage"))).isEqualTo(Outcome.Success(WordId("word.run")))
        assertThat(repository.getWordOf(SenseId("sense.walk.v.go"))).isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
    }

    @Test
    fun getItems_keepsTheAskedOrderAndDropsMissingItems() = runTest {
        val items = repository.getItems(
            listOf(
                SoundId("sound.f"),
                SenseId("sense.walk.v.go"),
                PatternId("pattern.thank-you"),
                SenseId("sense.run.v.manage")
            )
        )

        assertThat(items).isEqualTo(
            Outcome.Success(
                listOf(
                    LessonItem(SoundId("sound.f"), "f, ph", "f", null),
                    LessonItem(PatternId("pattern.thank-you"), "Thank you.", null, "고마워요"),
                    LessonItem(SenseId("sense.run.v.manage"), "run", "rʌn", "운영하다")
                )
            )
        )
    }

    @Test
    fun search_englishPrefix_findsFormsWithTheHeadwordFirst() = runTest {
        val hits = repository.search("  RUN ", limit = 10).ids()

        // "runner" also starts with "run", but the headword itself comes first.
        assertThat(hits).containsExactly("word.run", "word.runner")
    }

    @Test
    fun search_englishForm_findsTheHeadword() = runTest {
        assertThat(repository.search("ran", limit = 10).ids()).containsExactly("word.run")
    }

    @Test
    fun search_englishMatchesPatternWords() = runTest {
        assertThat(repository.search("you", limit = 10).ids()).containsExactly("pattern.thank-you")
        assertThat(repository.search("thank", limit = 10).ids()).containsExactly("pattern.thank-you")
    }

    @Test
    fun search_typographicApostrophe_matchesTheStraightOne() = runTest {
        assertThat(repository.search("I’m", limit = 10).ids()).containsExactly("pattern.im-fine")
    }

    @Test
    fun search_korean_findsMeaningsAnywhereAndListsEveryMeaning() = runTest {
        val hits = (repository.search("운영", limit = 10) as Outcome.Success).data

        assertThat(hits).containsExactly(
            SearchHit(
                id = WordId("word.run"),
                text = "run",
                ipa = "rʌn",
                meanings = listOf(
                    ItemMeaning(SenseId("sense.run.v.move-fast"), PartOfSpeech.VERB, "달리다", one),
                    ItemMeaning(SenseId("sense.run.v.manage"), PartOfSpeech.VERB, "운영하다", two)
                )
            )
        )
    }

    @Test
    fun search_ordersByEarliestStageThenAlphabet_andKeepsTheLimit() = runTest {
        // "고마워요" (stage one) and "고르다" (stage two) both contain "고".
        assertThat(repository.search("고", limit = 10).ids()).containsExactly("pattern.thank-you", "word.pick")
        assertThat(repository.search("고", limit = 1).ids()).containsExactly("pattern.thank-you")
    }

    @Test
    fun search_likeWildcards_areMatchedLiterally() = runTest {
        assertThat(repository.search("%", limit = 10).ids()).isEmpty()
        assertThat(repository.search("_", limit = 10).ids()).isEmpty()
    }

    @Test
    fun search_blankQuery_findsNothing() = runTest {
        assertThat(repository.search("   ", limit = 10).ids()).isEmpty()
    }

    @Test
    fun getSentence_returnsWordsWithTheirSenseChunksGlossAndGrammar() = runTest {
        val breakdown = (repository.getSentence(1) as Outcome.Success).data

        assertThat(breakdown.sentence).isEqualTo(Sentence(1, "We run every day.", "우리는 매일 달려요."))
        // "We" and "every" are not in the dictionary; "day" is, without a sense.
        assertThat(breakdown.words).containsExactly(
            SentenceWord(
                start = 3,
                end = 6,
                wordId = WordId("word.run"),
                headword = "run",
                sense = ItemMeaning(SenseId("sense.run.v.move-fast"), PartOfSpeech.VERB, "달리다", one)
            ),
            SentenceWord(start = 13, end = 16, wordId = WordId("word.day"), headword = "day", sense = null)
        )
        assertThat(breakdown.chunks).containsExactly(
            SentenceChunk(0, 2, ChunkRole.WHO),
            SentenceChunk(3, 6, ChunkRole.DO),
            SentenceChunk(7, 16, ChunkRole.WHEN)
        )
        assertThat(breakdown.literalKo).isEqualTo("우리는 / 달린다 / 매일")
        assertThat(breakdown.grammar).isEqualTo(
            GrammarNote("grammar.present", "현재형", listOf("늘 하는 일을 말해요.", "`every day`와 자주 써요."))
        )
    }

    @Test
    fun getSentence_withoutChunksOrGrammar_returnsEmptyParts() = runTest {
        val breakdown = (repository.getSentence(2) as Outcome.Success).data

        assertThat(breakdown.chunks).isEmpty()
        assertThat(breakdown.literalKo).isEqualTo(null)
        assertThat(breakdown.grammar).isEqualTo(null)
    }

    @Test
    fun getSentence_unknownId_notFound() = runTest {
        assertThat(repository.getSentence(99)).isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
    }

    @Test
    fun getSentence_linkToAMissingWord_unavailable() = runTest {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO sentence (id, text, translationKo, tokens, chunks, literalKo, grammarId) " +
                """VALUES (9, 'Go.', NULL, '[{"start":0,"end":2,"word":"word.go"}]', NULL, NULL, NULL)"""
        )

        assertThat(repository.getSentence(9)).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }

    @Test
    fun getEntry_databaseCannotOpen_unavailable() = runTest {
        val broken = RoomLexiconRepository { error("disk") }

        assertThat(broken.getEntry(WordId("word.run"))).isInstanceOf(Outcome.Failure::class)
        assertThat(broken.search("run", 10)).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }

    @Test
    fun search_moreMatchesThanSqliteBindsAtOnce_readsTheirMeaningsInParts() = runTest {
        database.openHelper.writableDatabase.apply {
            repeat(1200) { index ->
                word("word.z$index", "z$index", "zi", null, " z$index")
                sense("sense.z$index.n.x", "word.z$index", 0, "noun", "제트", "NULL", "stage.one")
            }
        }

        val hits = (repository.search("z", limit = 2000) as Outcome.Success).data

        assertThat(hits.size).isEqualTo(1200)
        assertThat(hits.all { it.meanings.size == 1 }).isEqualTo(true)
    }

    @Test
    fun getSentence_wordRangeOutsideTheText_unavailable() = runTest {
        database.openHelper.writableDatabase.execSQL(
            "INSERT INTO sentence (id, text, translationKo, tokens, chunks, literalKo, grammarId) " +
                """VALUES (9, 'Run.', NULL, '[{"start":0,"end":9,"word":"word.run"}]', NULL, NULL, NULL)"""
        )

        assertThat(repository.getSentence(9)).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }

    private fun Outcome<List<SearchHit>, ContentError>.ids(): List<String> =
        (this as Outcome.Success).data.map { it.id.value }

    private fun SupportSQLiteDatabase.fill() {
        execSQL("INSERT INTO stage (id, position, titleKo, titleEn) VALUES ('stage.two', 2, '둘', 'Two')")
        execSQL("INSERT INTO stage (id, position, titleKo, titleEn) VALUES ('stage.one', 1, '하나', 'One')")
        word("word.runner", "runner", "ˈrʌnər", null, " runner runners")
        word("word.run", "run", "rʌn", "'런'", " run runs ran running")
        word("word.day", "day", "deɪ", null, " day days")
        word("word.pick", "pick", "pɪk", null, " pick picks")
        sense("sense.run.v.manage", "word.run", 1, "verb", "운영하다", "'to be in charge of'", "stage.two")
        sense("sense.run.v.move-fast", "word.run", 0, "verb", "달리다", "NULL", "stage.one")
        sense("sense.runner.n.person", "word.runner", 0, "noun", "달리는 사람", "NULL", "stage.two")
        sense("sense.day.n.time", "word.day", 0, "noun", "날, 하루", "NULL", "stage.one")
        sense("sense.pick.v.choose", "word.pick", 0, "verb", "고르다", "NULL", "stage.two")
        execSQL(
            "INSERT INTO sentence (id, text, translationKo, tokens, chunks, literalKo, grammarId) VALUES " +
                "(1, 'We run every day.', '우리는 매일 달려요.', " +
                "'[{\"start\":0,\"end\":2},{\"start\":3,\"end\":6,\"word\":\"word.run\"," +
                "\"sense\":\"sense.run.v.move-fast\"},{\"start\":7,\"end\":12},{\"start\":13,\"end\":16," +
                "\"word\":\"word.day\"}]', " +
                "'[{\"start\":0,\"end\":2,\"role\":\"who\"},{\"start\":3,\"end\":6,\"role\":\"do\"}," +
                "{\"start\":7,\"end\":16,\"role\":\"when\"}]', '우리는 / 달린다 / 매일', 'grammar.present')"
        )
        execSQL(
            "INSERT INTO sentence (id, text, translationKo, tokens, chunks, literalKo, grammarId) VALUES " +
                "(2, 'I run.', '나는 달려요.', '[]', NULL, NULL, NULL)"
        )
        execSQL(
            "INSERT INTO sentence (id, text, translationKo, tokens, chunks, literalKo, grammarId) VALUES " +
                "(3, 'Thank you, Sam.', '고마워요, 샘.', '[]', NULL, NULL, NULL)"
        )
        execSQL(
            "INSERT INTO grammar (id, titleKo, bodyKo) " +
                "VALUES ('grammar.present', '현재형', '늘 하는 일을 말해요.\n\n`every day`와 자주 써요.')"
        )
        execSQL("INSERT INTO sense_example (senseId, position, sentenceId) VALUES ('sense.run.v.move-fast', 1, 1)")
        execSQL("INSERT INTO sense_example (senseId, position, sentenceId) VALUES ('sense.run.v.move-fast', 0, 2)")
        execSQL(
            "INSERT INTO word_collocation (wordId, position, textEn, textKo) VALUES ('word.run', 0, 'run a shop', '가게를 운영하다')"
        )
        execSQL(
            "INSERT INTO word_family (wordId, position, text, relatedWordId) VALUES ('word.run', 1, 'running', NULL)"
        )
        execSQL(
            "INSERT INTO word_family (wordId, position, text, relatedWordId) VALUES ('word.run', 0, 'runner', 'word.runner')"
        )
        execSQL(
            "INSERT INTO word_note (wordId, position, kind, textKo) " +
                "VALUES ('word.run', 1, 'konglish', '`running machine`은 콩글리시예요.')"
        )
        execSQL(
            "INSERT INTO word_note (wordId, position, kind, textKo) VALUES ('word.run', 0, 'mistake', '`run`의 과거형은 `ran`이에요.')"
        )
        execSQL(
            "INSERT INTO pattern (id, text, meaningKo, noteKo, stageId, lessonId) " +
                "VALUES ('pattern.thank-you', 'Thank you.', '고마워요', '`Thanks.`보다 조금 더 정중해요.', 'stage.one', NULL)"
        )
        execSQL(
            "INSERT INTO pattern (id, text, meaningKo, noteKo, stageId, lessonId) " +
                "VALUES ('pattern.im-fine', 'I’m fine.', '잘 지내요', NULL, 'stage.one', NULL)"
        )
        execSQL("INSERT INTO pattern_example (patternId, position, sentenceId) VALUES ('pattern.thank-you', 0, 3)")
        execSQL(
            "INSERT INTO sound (id, symbol, spelling, descriptionKo, stageId, lessonId) " +
                "VALUES ('sound.f', 'f', 'f, ph', '윗니를 아랫입술에 대요.', 'stage.one', NULL)"
        )
        execSQL("INSERT INTO sound_example (soundId, position, text, ipa) VALUES ('sound.f', 1, 'phone', 'foʊn')")
        execSQL("INSERT INTO sound_example (soundId, position, text, ipa) VALUES ('sound.f', 0, 'fan', 'fæn')")
    }

    private fun SupportSQLiteDatabase.word(id: String, text: String, ipa: String, hangul: String?, searchKey: String) =
        execSQL(
            "INSERT INTO word (id, text, ipa, hangul, searchKey) " +
                "VALUES ('$id', '$text', '$ipa', ${hangul ?: "NULL"}, '$searchKey')"
        )

    private fun SupportSQLiteDatabase.sense(
        id: String,
        wordId: String,
        position: Int,
        pos: String,
        meaningKo: String,
        definitionEn: String,
        stageId: String
    ) = execSQL(
        "INSERT INTO sense (id, wordId, position, pos, meaningKo, definitionEn, ipa, stageId, lessonId) " +
            "VALUES ('$id', '$wordId', $position, '$pos', '$meaningKo', $definitionEn, NULL, '$stageId', NULL)"
    )
}
