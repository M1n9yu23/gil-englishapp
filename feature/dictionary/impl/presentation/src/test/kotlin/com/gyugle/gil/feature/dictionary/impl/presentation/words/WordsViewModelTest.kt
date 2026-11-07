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
package com.gyugle.gil.feature.dictionary.impl.presentation.words

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.dictionary.impl.presentation.TestEntries
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ItemRowUi
import com.gyugle.gil.feature.dictionary.impl.presentation.component.LocalizedTextUi
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.Mistake
import com.gyugle.gil.feature.progress.api.ProgressObserver
import com.gyugle.gil.feature.progress.api.StudyDay
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WordsViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val lexicon = FakeLexiconObserver(TestEntries.run, TestEntries.thankYou, TestEntries.f)
    private val progress = FakeProgressObserver()
    private val handle = SavedStateHandle()

    @BeforeEach fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = WordsViewModel(handle, lexicon, progress)

    private val moveFastRow = ItemRowUi("sense.run.v.move-fast", "run", "rʌn", "달리다")
    private val thankYouRow = ItemRowUi("pattern.thank-you", "Thank you.", null, "고마워요")

    @Test
    fun init_nothingSaved_showsAnEmptyWordListOnTheFirstTab() = runTest(dispatcher) {
        val state = viewModel().state.value

        assertThat(state.tab).isEqualTo(WordsTab.SAVED)
        assertThat(state.saved).isEqualTo(WordListUi.Loaded(emptyList()))
        assertThat(state.mistakes).isEqualTo(WordListUi.Loaded(emptyList()))
        assertThat(state.search).isEqualTo(SearchUi.Idle)
    }

    @Test
    fun savedItems_listInTheOrderProgressGives_andFollowChanges() = runTest(dispatcher) {
        progress.savedItems.value = listOf(TestEntries.thankYou.id, TestEntries.moveFast.id)
        val viewModel = viewModel()

        assertThat(viewModel.state.value.saved).isEqualTo(WordListUi.Loaded(listOf(thankYouRow, moveFastRow)))

        progress.savedItems.value = listOf(TestEntries.moveFast.id)

        assertThat(viewModel.state.value.saved).isEqualTo(WordListUi.Loaded(listOf(moveFastRow)))
    }

    @Test
    fun mistakes_showHowOftenEachWentWrong() = runTest(dispatcher) {
        progress.mistakes.value = listOf(
            Mistake(TestEntries.f.id, count = 3, lastDay = StudyDay(10)),
            Mistake(TestEntries.moveFast.id, count = 1, lastDay = StudyDay(9))
        )

        val state = viewModel().state.value

        assertThat(state.mistakes).isEqualTo(
            WordListUi.Loaded(
                listOf(
                    ItemRowUi("sound.f", "f, ph", "f", null, mistakes = 3, isSound = true),
                    moveFastRow.copy(mistakes = 1)
                )
            )
        )
    }

    @Test
    fun contentUnavailable_listsFail_andRetryReadsAgain() = runTest(dispatcher) {
        progress.savedItems.value = listOf(TestEntries.moveFast.id)
        lexicon.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat(viewModel.state.value.saved).isInstanceOf(WordListUi.Failed::class)
        assertThat(viewModel.state.value.mistakes).isInstanceOf(WordListUi.Failed::class)

        lexicon.error = null
        viewModel.onAction(WordsAction.OnRetryClick)

        assertThat(viewModel.state.value.saved).isEqualTo(WordListUi.Loaded(listOf(moveFastRow)))
    }

    @Test
    fun recordUnreadable_listsFail_andSearchStillWorksWithNothingLearned() = runTest(dispatcher) {
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.thankYou))
        val broken = object : ProgressObserver by progress {
            override fun observeSavedItems(): Flow<List<com.gyugle.gil.feature.curriculum.api.StudyItemId>> =
                flow { throw IOException("disk") }

            override fun observeMistakes(): Flow<List<Mistake>> = flow { throw IOException("disk") }

            override fun observeCards() = flow<List<com.gyugle.gil.feature.progress.api.CardState>> {
                throw IOException("disk")
            }
        }
        val viewModel = WordsViewModel(handle, lexicon, broken)

        viewModel.onAction(WordsAction.OnQueryChange("thank"))
        advanceTimeBy(300)

        assertThat(viewModel.state.value.saved).isInstanceOf(WordListUi.Failed::class)
        assertThat(viewModel.state.value.mistakes).isInstanceOf(WordListUi.Failed::class)
        assertThat(viewModel.state.value.search).isEqualTo(
            SearchUi.Results(
                learned = emptyList(),
                notLearned = listOf(
                    thankYouRow.copy(stage = LocalizedTextUi("첫걸음", "First steps"))
                )
            )
        )
    }

    @Test
    fun mistakesUnavailable_retry_readsThemAgain() = runTest(dispatcher) {
        progress.mistakes.value = listOf(Mistake(TestEntries.moveFast.id, count = 2, lastDay = StudyDay(1)))
        lexicon.error = ContentError.UNAVAILABLE
        val viewModel = viewModel()
        assertThat(viewModel.state.value.mistakes).isInstanceOf(WordListUi.Failed::class)

        lexicon.error = null
        viewModel.onAction(WordsAction.OnRetryClick)

        assertThat(viewModel.state.value.mistakes).isEqualTo(WordListUi.Loaded(listOf(moveFastRow.copy(mistakes = 2))))
    }

    @Test
    fun searchFailed_retry_searchesAgain() = runTest(dispatcher) {
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.run))
        val viewModel = viewModel()
        lexicon.error = ContentError.UNAVAILABLE
        viewModel.onAction(WordsAction.OnQueryChange("run"))
        advanceTimeBy(300)
        assertThat(viewModel.state.value.search).isInstanceOf(SearchUi.Failed::class)

        lexicon.error = null
        viewModel.onAction(WordsAction.OnRetryClick)

        assertThat(viewModel.state.value.search).isInstanceOf(SearchUi.Results::class)
    }

    @Test
    fun learningAWord_reordersTheResults_withoutSearchingAgain() = runTest(dispatcher) {
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.run))
        val viewModel = viewModel()
        viewModel.onAction(WordsAction.OnQueryChange("run"))
        advanceTimeBy(300)

        progress.learn(TestEntries.moveFast.id)

        assertThat(lexicon.searches).containsExactly("run")
        assertThat((viewModel.state.value.search as SearchUi.Results).learned.map { it.id }).containsExactly("word.run")
    }

    @Test
    fun koreanSearch_putsTheMatchingMeaningFirst() = runTest(dispatcher) {
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.run))
        val viewModel = viewModel()

        viewModel.onAction(WordsAction.OnQueryChange("운영"))
        advanceTimeBy(300)

        val row = (viewModel.state.value.search as SearchUi.Results).notLearned.single()
        assertThat(row.meaningKo).isEqualTo("운영하다, 달리다")
    }

    @Test
    fun onTabSelect_showsTheTabAndKeepsItForProcessDeath() = runTest(dispatcher) {
        viewModel().onAction(WordsAction.OnTabSelect(WordsTab.MISTAKES))

        assertThat(viewModel().state.value.tab).isEqualTo(WordsTab.MISTAKES)
    }

    @Test
    fun onQueryChange_updatesAtOnce_andSearchesAfterAPause() = runTest(dispatcher) {
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.run))
        val viewModel = viewModel()

        viewModel.onAction(WordsAction.OnQueryChange("ru"))
        viewModel.onAction(WordsAction.OnQueryChange("run "))

        assertThat(viewModel.state.value.query).isEqualTo("run ")
        assertThat(viewModel.state.value.search).isEqualTo(SearchUi.Idle)
        advanceTimeBy(300)
        // Only the text the learner stopped at was searched.
        assertThat(lexicon.searches).containsExactly("run ")
        assertThat(viewModel.state.value.search).isInstanceOf(SearchUi.Results::class)
    }

    @Test
    fun search_learnedWordsComeFirst_andTheOthersNameTheirStage() = runTest(dispatcher) {
        progress.learn(TestEntries.thankYou.id)
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.run), TestEntries.hit(TestEntries.thankYou))
        val viewModel = viewModel()

        viewModel.onAction(WordsAction.OnQueryChange("r"))
        advanceTimeBy(300)

        assertThat(viewModel.state.value.search).isEqualTo(
            SearchUi.Results(
                learned = listOf(thankYouRow),
                notLearned = listOf(
                    ItemRowUi(
                        "word.run",
                        "run",
                        "rʌn",
                        "달리다, 운영하다",
                        stage = LocalizedTextUi("첫걸음", "First steps")
                    )
                )
            )
        )
    }

    @Test
    fun search_noHits_saysWhatWasNotFound() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(WordsAction.OnQueryChange(" tabel "))
        advanceTimeBy(300)

        assertThat(viewModel.state.value.search).isEqualTo(SearchUi.NoResults("tabel"))
    }

    @Test
    fun search_contentUnavailable_fails() = runTest(dispatcher) {
        val viewModel = viewModel()
        lexicon.error = ContentError.UNAVAILABLE

        viewModel.onAction(WordsAction.OnQueryChange("run"))
        advanceTimeBy(300)

        assertThat(viewModel.state.value.search).isInstanceOf(SearchUi.Failed::class)
    }

    @Test
    fun onQueryClear_goesBackToTheHintAtOnce() = runTest(dispatcher) {
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.run))
        val viewModel = viewModel()
        viewModel.onAction(WordsAction.OnQueryChange("run"))
        advanceTimeBy(300)

        viewModel.onAction(WordsAction.OnQueryClear)

        assertThat(viewModel.state.value.query).isEqualTo("")
        assertThat(viewModel.state.value.search).isEqualTo(SearchUi.Idle)
    }

    @Test
    fun init_afterProcessDeath_restoresTheTabAndSearchesTheSavedQuery() = runTest(dispatcher) {
        lexicon.searchHits = listOf(TestEntries.hit(TestEntries.run))
        handle["tab"] = WordsTab.SEARCH.name
        handle["query"] = "run"

        val viewModel = viewModel()
        advanceTimeBy(300)

        assertThat(viewModel.state.value.tab).isEqualTo(WordsTab.SEARCH)
        assertThat(viewModel.state.value.query).isEqualTo("run")
        assertThat(lexicon.searches).containsExactly("run")
    }
}
