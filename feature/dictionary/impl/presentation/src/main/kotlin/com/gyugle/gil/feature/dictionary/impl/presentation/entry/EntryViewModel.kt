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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.EntryId
import com.gyugle.gil.feature.curriculum.api.LexiconEntry
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.dictionary.impl.presentation.toContentErrorUi
import com.gyugle.gil.feature.progress.api.ProgressCommands
import com.gyugle.gil.feature.progress.api.ProgressObserver
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

@HiltViewModel(assistedFactory = EntryViewModel.Factory::class)
class EntryViewModel @AssistedInject constructor(
    @Assisted private val entryId: String,
    private val lexicon: LexiconObserver,
    private val progressObserver: ProgressObserver,
    private val progressCommands: ProgressCommands,
    private val speaker: Speaker
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(entryId: String): EntryViewModel
    }

    private val _state = MutableStateFlow(EntryState())
    val state: StateFlow<EntryState> = _state.asStateFlow()

    private val eventChannel = Channel<EntryEvent>()
    val events = eventChannel.receiveAsFlow()

    /** Bumped by a retry, so the page reads the content again. */
    private val attempts = MutableStateFlow(0)
    private var playing: Job? = null

    private var loaded: Pair<LexiconEntry, SenseId?>? = null
    private var learned: Set<StudyItemId> = emptySet()

    /**
     * The word list as the page shows it. A toggle changes it at once, before the record confirms,
     * so a second tap in quick succession toggles back instead of repeating the first.
     */
    private var saved: Set<StudyItemId> = emptySet()
    private val saving = Mutex()

    init {
        viewModelScope.launch {
            attempts.collectLatest {
                _state.update { it.copy(isLoading = true, error = null) }
                when (val result = load()) {
                    is Outcome.Failure -> {
                        _state.update { it.copy(isLoading = false, error = result.error.toContentErrorUi()) }
                        return@collectLatest
                    }

                    is Outcome.Success -> loaded = result.data
                }
                val cards = progressObserver.observeCards().map { list -> list.map { it.itemId }.toSet() }
                combine(cards, progressObserver.observeSavedItems()) { known, list -> known to list.toSet() }
                    .catch { e ->
                        Timber.e(e, "Could not read the learner's record")
                        _state.update {
                            it.copy(isLoading = false, error = ContentError.UNAVAILABLE.toContentErrorUi())
                        }
                    }
                    .collect { (known, list) ->
                        learned = known
                        // A toggle in flight owns the shown list until the record has caught up.
                        if (!saving.isLocked) saved = list
                        render()
                    }
            }
        }
        viewModelScope.launch {
            speaker.status.collect { status -> _state.update { it.copy(voices = status.toVoicesUi()) } }
        }
    }

    fun onAction(action: EntryAction) {
        when (action) {
            EntryAction.OnRetryClick -> attempts.update { it + 1 }
            is EntryAction.OnPlayClick -> play(action.text, action.accent)
            is EntryAction.OnSaveToggle -> toggleSaved(action.itemId)
        }
    }

    private fun render() {
        val (entry, focus) = loaded ?: return
        _state.update { it.copy(isLoading = false, entry = entry.toEntryUi(learned, saved, focus)) }
    }

    /** The entry and, when the route named a sense, that sense. */
    private suspend fun load(): Outcome<Pair<LexiconEntry, SenseId?>, ContentError> {
        EntryId.parse(entryId)?.let { id ->
            return when (val entry = lexicon.getEntry(id)) {
                is Outcome.Success -> Outcome.Success(entry.data to null)
                is Outcome.Failure -> entry
            }
        }
        val sense = StudyItemId.parse(entryId) as? SenseId ?: return Outcome.Failure(ContentError.NOT_FOUND)
        val word = when (val owner = lexicon.getWordOf(sense)) {
            is Outcome.Success -> owner.data
            is Outcome.Failure -> return owner
        }
        return when (val entry = lexicon.getEntry(word)) {
            is Outcome.Success -> Outcome.Success(entry.data to sense)
            is Outcome.Failure -> entry
        }
    }

    private fun play(text: String, accent: Accent) {
        playing?.cancel()
        playing = viewModelScope.launch {
            val language = when (accent) {
                Accent.AMERICAN -> SpeechLanguage.AMERICAN_ENGLISH
                Accent.BRITISH -> SpeechLanguage.BRITISH_ENGLISH
            }
            // The button only shows with a voice; a failure now has nothing to offer but silence.
            speaker.speak(text, language).onFailure { Timber.w("Could not speak an entry: %s", it) }
        }
    }

    private fun toggleSaved(itemId: String) {
        val item = StudyItemId.parse(itemId) ?: return
        val save = item !in saved
        saved = if (save) saved + item else saved - item
        render()
        viewModelScope.launch {
            saving.withLock {
                progressCommands.setSaved(item, save).onFailure { error ->
                    saved = if (save) saved - item else saved + item
                    render()
                    eventChannel.send(EntryEvent.ShowMessage(error.toUiText()))
                }
            }
        }
    }
}
