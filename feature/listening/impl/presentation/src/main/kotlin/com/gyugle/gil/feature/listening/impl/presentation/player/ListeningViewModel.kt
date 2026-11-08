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
package com.gyugle.gil.feature.listening.impl.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.core.presentation.toUiText
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.listening.impl.domain.ListeningLibrary
import com.gyugle.gil.feature.listening.impl.domain.ListeningPlayer
import com.gyugle.gil.feature.listening.impl.domain.ListeningPreferences
import com.gyugle.gil.feature.listening.impl.domain.TrackRenderer
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningChoices
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackSession
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderError
import com.gyugle.gil.feature.listening.impl.domain.model.RenderProgress
import com.gyugle.gil.feature.listening.impl.domain.trackOf
import com.gyugle.gil.feature.listening.impl.presentation.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * Listening mode. What is playing lives in the background player, so leaving the screen and coming
 * back, or opening it from the notification, shows the same playback.
 */
@HiltViewModel
class ListeningViewModel @Inject constructor(
    private val library: ListeningLibrary,
    private val renderer: TrackRenderer,
    private val player: ListeningPlayer,
    private val preferences: ListeningPreferences,
    private val speaker: Speaker
) : ViewModel() {

    private val choices = MutableStateFlow<ChoicesRead>(ChoicesRead.Loading)
    private val preparing = MutableStateFlow<ListeningState.Preparing?>(null)
    private val dialog = MutableStateFlow<PaceDialog?>(null)
    private var prepareJob: Job? = null

    val state: StateFlow<ListeningState> = combine(
        combine(choices, preferences.observePace(), preferences.observeLastPlayed(), ::Picker),
        speaker.status,
        preparing,
        dialog,
        player.session
    ) { picker, status, preparing, dialog, session -> stateOf(picker, status, preparing, dialog, session) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), ListeningState.Loading)

    private val eventChannel = Channel<ListeningEvent>()
    val events: Flow<ListeningEvent> = eventChannel.receiveAsFlow()

    init {
        loadChoices()
        player.failures.onEach { message(R.string.listening_message_playback_failed) }.launchIn(viewModelScope)
    }

    fun onAction(action: ListeningAction) {
        when (action) {
            is ListeningAction.OnSourceClick -> start(action.source, trackId = null)

            ListeningAction.OnResumeClick -> viewModelScope.launch {
                preferences.observeLastPlayed().first()?.let { start(it.source, it.trackId) }
            }

            ListeningAction.OnSpeedClick -> dialog.value = PaceDialog.SPEED

            ListeningAction.OnPauseLengthClick -> dialog.value = PaceDialog.PAUSE

            ListeningAction.OnDialogDismiss -> dialog.value = null

            is ListeningAction.OnSpeedPick -> savePace { it.copy(speed = action.speed) }

            is ListeningAction.OnPauseLengthPick -> savePace { it.copy(pause = action.pause) }

            ListeningAction.OnInstallClick -> send(ListeningEvent.OpenVoiceInstall)

            ListeningAction.OnInstallUnavailable -> message(R.string.listening_install_unavailable)

            ListeningAction.OnCancelPreparingClick -> prepareJob?.cancel()

            ListeningAction.OnPlayPauseClick -> if (player.session.value?.playing ==
                true
            ) {
                player.pause()
            } else {
                player.resume()
            }

            ListeningAction.OnPreviousClick -> player.skipToPrevious()

            ListeningAction.OnNextClick -> player.skipToNext()

            ListeningAction.OnStopClick -> {
                player.stop()
                loadChoices()
            }

            ListeningAction.OnPlayAgainClick -> player.resume()

            ListeningAction.OnRetryClick -> {
                choices.value = ChoicesRead.Loading
                loadChoices()
            }

            ListeningAction.OnResumed -> {
                speaker.refresh()
                loadChoices()
            }
        }
    }

    /** Keeps what is shown while reading again, so coming back to the screen does not flash. */
    private fun loadChoices() {
        viewModelScope.launch {
            choices.value = when (val read = readSafely { library.choices() }) {
                is Outcome.Success -> ChoicesRead.Ready(read.data)
                is Outcome.Failure -> ChoicesRead.Failed
            }
        }
    }

    private fun start(source: PlaylistSource, trackId: String?) {
        if (prepareJob?.isActive == true) return
        prepareJob = viewModelScope.launch {
            preparing.value = ListeningState.Preparing(done = 0, total = 0)
            try {
                prepareAndPlay(source, trackId)
            } finally {
                preparing.value = null
            }
        }
    }

    private suspend fun prepareAndPlay(source: PlaylistSource, trackId: String?) {
        val entries = when (val read = readSafely { library.entries(source) }) {
            is Outcome.Success -> read.data
            is Outcome.Failure -> return message(R.string.listening_message_load_failed)
        }
        if (entries.isEmpty()) return message(R.string.listening_message_load_failed)
        val pace = preferences.observePace().first()
        val tracks = entries.map {
            trackOf(it, pace.pause, withKorean = speaker.status.value.has(SpeechLanguage.KOREAN))
        }
        val result = renderer.render(tracks, pace.speed)
            .onEach { if (it is RenderProgress.Writing) preparing.value = ListeningState.Preparing(it.done, it.total) }
            .last()
        when (result) {
            is RenderProgress.Done -> {
                val start = tracks.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
                when (player.play(source, result.tracks, start)) {
                    // Stays on "preparing" until the player reports in, so the list does not flash.
                    is Outcome.Success -> withTimeoutOrNull(PLAYER_WAIT) { player.session.first { it != null } }

                    is Outcome.Failure -> message(R.string.listening_message_play_failed)
                }
            }

            is RenderProgress.Failed -> message(
                if (result.error == RenderError.VOICE_NOT_INSTALLED) {
                    R.string.listening_message_voice_missing
                } else {
                    R.string.listening_message_render_failed
                }
            )

            is RenderProgress.Writing -> Unit
        }
    }

    private fun savePace(change: (ListeningPace) -> ListeningPace) {
        dialog.value = null
        viewModelScope.launch {
            val pace = preferences.observePace().first()
            preferences.setPace(change(pace)).onFailure { send(ListeningEvent.ShowMessage(it.toUiText())) }
        }
    }

    /** The record's flows throw when the database cannot be read; that is shown like unreadable content. */
    private suspend fun <T> readSafely(read: suspend () -> Outcome<T, ContentError>): Outcome<T, ContentError> = try {
        read()
    } catch (error: Exception) {
        currentCoroutineContext().ensureActive()
        Timber.w(error, "Listening content not read")
        Outcome.Failure(ContentError.UNAVAILABLE)
    }

    private fun message(text: Int) = send(ListeningEvent.ShowMessage(UiText.StringResource(text)))

    private fun send(event: ListeningEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun stateOf(
        picker: Picker,
        status: SpeakerStatus,
        preparing: ListeningState.Preparing?,
        dialog: PaceDialog?,
        session: PlaybackSession?
    ): ListeningState {
        session?.let { return it.toState() }
        preparing?.let { return it }
        when (status) {
            SpeakerStatus.Connecting -> return ListeningState.Loading
            SpeakerStatus.NoEngine -> return ListeningState.NoEngine
            is SpeakerStatus.Connected -> Unit
        }
        if (!status.has(SpeechLanguage.AMERICAN_ENGLISH) && !status.has(SpeechLanguage.BRITISH_ENGLISH)) {
            return ListeningState.NoEnglishVoice
        }
        return when (val read = picker.choices) {
            ChoicesRead.Loading -> ListeningState.Loading

            ChoicesRead.Failed -> ListeningState.Failed

            is ChoicesRead.Ready -> if (read.choices.isEmpty) {
                ListeningState.NothingYet
            } else {
                read.choices.toChoosing(picker, koreanVoice = status.has(SpeechLanguage.KOREAN), dialog)
            }
        }
    }

    private fun ListeningChoices.toChoosing(picker: Picker, koreanVoice: Boolean, dialog: PaceDialog?): ListeningState {
        val sources = listOf(
            SourceUi(PlaylistSource.DueToday, SourceName.DueToday, dueToday),
            SourceUi(PlaylistSource.Recent, SourceName.Recent, recent),
            SourceUi(PlaylistSource.Dialogues, SourceName.Dialogues, dialogues)
        ).filter { it.count > 0 }
        val units = units.map { SourceUi(PlaylistSource.CourseUnit(it.id), it.title.toSourceName(), it.itemCount) }
        return ListeningState.Choosing(
            sources = sources,
            units = units,
            resume = picker.lastPlayed?.let { last -> (sources + units).firstOrNull { it.source == last.source } },
            pace = picker.pace,
            koreanVoice = koreanVoice,
            dialog = dialog
        )
    }

    private fun PlaybackSession.toState(): ListeningState {
        if (ended) return ListeningState.Ended(tracks.size)
        val track = tracks.getOrNull(index) ?: return ListeningState.Loading
        return ListeningState.Playing(TrackUi(track.title, track.subtitle), index + 1, tracks.size, playing)
    }

    private fun SpeakerStatus.has(language: SpeechLanguage): Boolean =
        this is SpeakerStatus.Connected && voice(language) == VoiceState.INSTALLED

    private class Picker(val choices: ChoicesRead, val pace: ListeningPace, val lastPlayed: LastPlayed?)

    private sealed interface ChoicesRead {
        data object Loading : ChoicesRead

        data object Failed : ChoicesRead

        data class Ready(val choices: ListeningChoices) : ChoicesRead
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        val PLAYER_WAIT = 2.seconds
    }
}
