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
package com.gyugle.gil.feature.dictionary.impl.presentation.sentence

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.core.speech.FakeSpeaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.feature.curriculum.api.ChunkRole
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.GrammarNote
import com.gyugle.gil.feature.curriculum.api.ItemMeaning
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.SentenceBreakdown
import com.gyugle.gil.feature.curriculum.api.SentenceChunk
import com.gyugle.gil.feature.curriculum.api.SentenceWord
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.dictionary.impl.presentation.R
import com.gyugle.gil.feature.dictionary.impl.presentation.TestEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SentenceViewModelTest {
    private val lexicon = FakeLexiconObserver()
    private val speaker = FakeSpeaker()

    private val breakdown = SentenceBreakdown(
        sentence = Sentence(7, "We run every day.", "우리는 매일 달려요."),
        words = listOf(
            SentenceWord(
                3,
                6,
                WordId("word.run"),
                "run",
                ItemMeaning(TestEntries.moveFast.id, PartOfSpeech.VERB, "달리다", TestEntries.firstSteps)
            ),
            SentenceWord(13, 16, WordId("word.day"), "day", null)
        ),
        chunks = listOf(SentenceChunk(0, 2, ChunkRole.WHO), SentenceChunk(7, 16, ChunkRole.WHEN)),
        literalKo = "우리는 / 달린다 / 매일",
        grammar = GrammarNote("grammar.present", "현재형", listOf("늘 하는 일을 말해요."))
    )

    @BeforeEach fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(id: Long = 7) = SentenceViewModel(id, lexicon, speaker)

    @Test
    fun init_knownSentence_mapsWordsChunksGlossAndGrammar() = runTest {
        lexicon.sentences += breakdown

        val state = viewModel().state.value

        assertThat(state.sentence).isEqualTo(
            SentenceUi(
                text = "We run every day.",
                ko = "우리는 매일 달려요.",
                words = listOf(
                    SentenceWordUi(3, 6, "word.run", "run", R.string.dictionary_pos_verb, "달리다"),
                    SentenceWordUi(13, 16, "word.day", "day", null, null)
                ),
                chunks = listOf(
                    ChunkUi("We", R.string.dictionary_role_who),
                    ChunkUi("every day", R.string.dictionary_role_when)
                ),
                // Three gloss parts do not line up with two chunks, so the gloss stays whole.
                literalKo = "우리는 / 달린다 / 매일",
                grammar = GrammarUi("현재형", listOf("늘 하는 일을 말해요."))
            )
        )
        assertThat(state.selected).isNull()
        assertThat(state.hasVoice).isEqualTo(true)
    }

    @Test
    fun init_glossWithOnePartPerChunk_putsEachPartUnderItsChunk() = runTest {
        lexicon.sentences += breakdown.copy(literalKo = "우리는 / 매일")

        val sentence = viewModel().state.value.sentence

        assertThat(sentence?.chunks).isEqualTo(
            listOf(
                ChunkUi("We", R.string.dictionary_role_who, glossKo = "우리는"),
                ChunkUi("every day", R.string.dictionary_role_when, glossKo = "매일")
            )
        )
        assertThat(sentence?.literalKo).isNull()
    }

    @Test
    fun onWordClick_selectsThatWord_andIgnoresAnIndexOutOfRange() = runTest {
        lexicon.sentences += breakdown
        val viewModel = viewModel()

        viewModel.onAction(SentenceAction.OnWordClick(1))
        assertThat(viewModel.state.value.selected).isEqualTo(1)

        viewModel.onAction(SentenceAction.OnWordClick(5))
        assertThat(viewModel.state.value.selected).isEqualTo(1)
    }

    @Test
    fun onPlayClick_speaksTheSentence() = runTest {
        lexicon.sentences += breakdown
        val viewModel = viewModel()

        viewModel.onAction(SentenceAction.OnPlayClick)

        assertThat(speaker.spoken).containsExactly(
            FakeSpeaker.Utterance("We run every day.", SpeechLanguage.AMERICAN_ENGLISH, SpeechRate.NORMAL)
        )
    }

    @Test
    fun init_noSpeechEngine_hidesPlaying() = runTest {
        speaker.status.value = SpeakerStatus.NoEngine
        lexicon.sentences += breakdown

        assertThat(viewModel().state.value.hasVoice).isEqualTo(false)
    }

    @Test
    fun init_unknownSentence_notFound_andRetryAfterUnavailableLoads() = runTest {
        assertThat(viewModel(99).state.value.error?.canRetry).isEqualTo(false)

        lexicon.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat(viewModel.state.value.error?.canRetry).isEqualTo(true)
        lexicon.error = null
        lexicon.sentences += breakdown

        viewModel.onAction(SentenceAction.OnRetryClick)

        assertThat(viewModel.state.value.error).isNull()
        assertThat(viewModel.state.value.sentence?.text).isEqualTo("We run every day.")
    }
}
