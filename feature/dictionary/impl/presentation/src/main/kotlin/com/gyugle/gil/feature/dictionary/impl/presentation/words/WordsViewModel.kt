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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.domain.flatMap
import com.gyugle.gil.core.domain.map
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.SearchHit
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.dictionary.impl.domain.learnedFirst
import com.gyugle.gil.feature.dictionary.impl.domain.mistakeEntries
import com.gyugle.gil.feature.dictionary.impl.presentation.toContentErrorUi
import com.gyugle.gil.feature.progress.api.ProgressObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class WordsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val lexicon: LexiconObserver,
    private val progress: ProgressObserver
) : ViewModel() {

    private val _state = MutableStateFlow(
        WordsState(
            tab = savedStateHandle.get<String>(KEY_TAB)?.let(WordsTab::valueOf) ?: WordsTab.SAVED,
            query = savedStateHandle[KEY_QUERY] ?: ""
        )
    )
    val state: StateFlow<WordsState> = _state.asStateFlow()

    private val query = MutableStateFlow(_state.value.query)

    /** Bumped by a retry, so every list reads the record and the content again. */
    private val attempts = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            attempts.flatMapLatest { progress.observeSavedItems().readFailures() }
                .mapLatest { ids -> ids.flatMap { lexicon.getItems(it) } }
                .collect { items ->
                    _state.update { it.copy(saved = items.toWordListUi { item -> item.toItemRowUi() }) }
                }
        }
        viewModelScope.launch {
            attempts.flatMapLatest { progress.observeMistakes().readFailures() }
                .mapLatest { mistakes ->
                    mistakes.flatMap { list ->
                        lexicon.getItems(list.map { it.itemId }).map { mistakeEntries(list, it) }
                    }
                }
                .collect { entries ->
                    _state.update { it.copy(mistakes = entries.toWordListUi { entry -> entry.toItemRowUi() }) }
                }
        }
        viewModelScope.launch {
            // Learning a word reorders the results already found; it does not search again.
            combine(searchResults(), learnedItems()) { results, learned -> results.toSearchUi(learned) }
                .collect { search -> _state.update { it.copy(search = search) } }
        }
    }

    fun onAction(action: WordsAction) {
        when (action) {
            is WordsAction.OnTabSelect -> {
                savedStateHandle[KEY_TAB] = action.tab.name
                _state.update { it.copy(tab = action.tab) }
            }

            is WordsAction.OnQueryChange -> setQuery(action.query)

            WordsAction.OnQueryClear -> setQuery("")

            WordsAction.OnRetryClick -> attempts.update { it + 1 }
        }
    }

    private fun setQuery(text: String) {
        savedStateHandle[KEY_QUERY] = text
        _state.update { it.copy(query = text) }
        query.value = text
    }

    /** Typing waits for a pause before it searches; clearing the field takes effect at once. */
    private fun searchResults(): Flow<SearchResults> {
        val typed = query.debounce { if (it.isBlank()) 0L else SEARCH_DELAY_MILLIS }.distinctUntilChanged()
        return combine(typed, attempts) { text, _ -> text }.mapLatest { text ->
            if (text.isBlank()) {
                SearchResults.Idle
            } else {
                when (val hits = lexicon.search(text, SEARCH_LIMIT)) {
                    is Outcome.Success -> SearchResults.Found(text.trim(), hits.data)
                    is Outcome.Failure -> SearchResults.Failed(hits.error)
                }
            }
        }
    }

    /** Without the record, search still works; every word just counts as not learned yet. */
    private fun learnedItems(): Flow<Set<StudyItemId>> = attempts.flatMapLatest {
        progress.observeCards()
            .map { cards -> cards.map { it.itemId }.toSet() }
            .catch { e ->
                Timber.e(e, "Could not read the learned items")
                emit(emptySet())
            }
    }.distinctUntilChanged()

    private fun SearchResults.toSearchUi(learned: Set<StudyItemId>): SearchUi = when (this) {
        SearchResults.Idle -> SearchUi.Idle
        is SearchResults.Found -> hits.learnedFirst(learned).toSearchUi(query)
        is SearchResults.Failed -> SearchUi.Failed(error.toContentErrorUi())
    }

    private sealed interface SearchResults {
        data object Idle : SearchResults

        data class Found(val query: String, val hits: List<SearchHit>) : SearchResults

        data class Failed(val error: ContentError) : SearchResults
    }

    private companion object {
        const val KEY_TAB = "tab"
        const val KEY_QUERY = "query"
        const val SEARCH_DELAY_MILLIS = 250L
        const val SEARCH_LIMIT = 50
    }
}

/** A record that cannot be read shows the same failure as content that cannot be read. */
private fun <T> Flow<T>.readFailures(): Flow<Outcome<T, ContentError>> =
    map<T, Outcome<T, ContentError>> { Outcome.Success(it) }.catch { e ->
        Timber.e(e, "Could not read the learner's record")
        emit(Outcome.Failure(ContentError.UNAVAILABLE))
    }
