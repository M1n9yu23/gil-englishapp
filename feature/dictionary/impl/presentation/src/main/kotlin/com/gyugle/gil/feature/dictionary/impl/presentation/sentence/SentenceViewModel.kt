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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.domain.onSuccess
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.dictionary.impl.presentation.entry.toVoicesUi
import com.gyugle.gil.feature.dictionary.impl.presentation.toContentErrorUi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel(assistedFactory = SentenceViewModel.Factory::class)
class SentenceViewModel @AssistedInject constructor(
    @Assisted private val sentenceId: Long,
    private val lexicon: LexiconObserver,
    private val speaker: Speaker
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(sentenceId: Long): SentenceViewModel
    }

    private val _state = MutableStateFlow(SentenceState())
    val state: StateFlow<SentenceState> = _state.asStateFlow()

    private var playing: Job? = null

    init {
        load()
        viewModelScope.launch {
            speaker.status.collect { status -> _state.update { it.copy(hasVoice = status.toVoicesUi().american) } }
        }
    }

    fun onAction(action: SentenceAction) {
        when (action) {
            is SentenceAction.OnWordClick -> _state.update { state ->
                val count = state.sentence?.words?.size ?: 0
                if (action.index in 0 until count) state.copy(selected = action.index) else state
            }

            SentenceAction.OnPlayClick -> play()

            SentenceAction.OnRetryClick -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            lexicon.getSentence(sentenceId)
                .onSuccess { breakdown ->
                    _state.update { it.copy(isLoading = false, sentence = breakdown.toSentenceUi()) }
                }
                .onFailure { error -> _state.update { it.copy(isLoading = false, error = error.toContentErrorUi()) } }
        }
    }

    private fun play() {
        val text = _state.value.sentence?.text ?: return
        playing?.cancel()
        playing = viewModelScope.launch {
            speaker.speak(text, SpeechLanguage.AMERICAN_ENGLISH)
                .onFailure { Timber.w("Could not speak a sentence: %s", it) }
        }
    }
}
