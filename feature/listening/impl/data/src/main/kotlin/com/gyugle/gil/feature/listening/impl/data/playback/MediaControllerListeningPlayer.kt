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
package com.gyugle.gil.feature.listening.impl.data.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.gyugle.gil.core.domain.ApplicationScope
import com.gyugle.gil.core.domain.EmptyOutcome
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.feature.listening.impl.domain.ListeningPlayer
import com.gyugle.gil.feature.listening.impl.domain.ListeningPreferences
import com.gyugle.gil.feature.listening.impl.domain.model.LastPlayed
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackError
import com.gyugle.gil.feature.listening.impl.domain.model.PlaybackSession
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

/**
 * Talks to [ListeningService] through a media controller, as the lock screen does, so the screen
 * and the system see the same playback. Every call runs on the main thread.
 *
 * Also remembers the track the learner is on, to resume there next time.
 */
@Singleton
internal class MediaControllerListeningPlayer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val queue: TrackQueue,
    private val preferences: ListeningPreferences,
    @param:ApplicationScope private val scope: CoroutineScope
) : ListeningPlayer {
    private val _session = MutableStateFlow<PlaybackSession?>(null)
    override val session: StateFlow<PlaybackSession?> = _session.asStateFlow()

    private val _failures = MutableSharedFlow<PlaybackError>(extraBufferCapacity = 1)
    override val failures: Flow<PlaybackError> = _failures.asSharedFlow()

    private var controller: MediaController? = null
    private var lastError: PlaybackException? = null

    /** One writer in order; a newer position replaces one not yet written. */
    private val resumePoints = Channel<LastPlayed?>(Channel.CONFLATED)

    init {
        scope.launch {
            for (lastPlayed in resumePoints) {
                preferences.setLastPlayed(lastPlayed).onFailure { Timber.w("Listening position not saved: %s", it) }
            }
        }
    }

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) = update(player)
    }

    /** The service went away, such as after the notification was dismissed while paused. */
    private val connection = object : MediaController.Listener {
        override fun onDisconnected(controller: MediaController) {
            if (this@MediaControllerListeningPlayer.controller === controller) {
                this@MediaControllerListeningPlayer.controller = null
                _session.value = null
            }
        }
    }

    override suspend fun play(
        source: PlaylistSource,
        tracks: List<RenderedTrack>,
        startIndex: Int
    ): EmptyOutcome<PlaybackError> {
        val controller = connect() ?: return Outcome.Failure(PlaybackError.UNAVAILABLE)
        queue.load(source, tracks)
        controller.setMediaItems(tracks.map(::requestItemOf), startIndex.coerceIn(tracks.indices), 0L)
        controller.prepare()
        controller.play()
        return Outcome.Success(Unit)
    }

    override fun pause() {
        controller?.pause()
    }

    override fun resume() {
        val controller = controller ?: return
        // After the end, playing again starts over.
        if (controller.playbackState == Player.STATE_ENDED) controller.seekTo(0, 0L)
        controller.play()
    }

    override fun skipToNext() {
        controller?.seekToNextMediaItem()
    }

    override fun skipToPrevious() {
        controller?.seekToPrevious()
    }

    /** Also lets go of the service, so it can stop once nothing is playing. */
    override fun stop() {
        controller?.run {
            stop()
            clearMediaItems()
            release()
        }
        controller = null
        _session.value = null
    }

    private suspend fun connect(): MediaController? {
        controller?.takeIf { it.isConnected }?.let { return it }
        val token = SessionToken(context, ComponentName(context, ListeningService::class.java))
        val future = MediaController.Builder(context, token).setListener(connection).buildAsync()
        val connected = suspendCancellableCoroutine<MediaController?> { continuation ->
            future.addListener(
                listener@{
                    // Cancelled by the caller, which has already gone.
                    if (future.isCancelled) return@listener
                    val result = try {
                        future.get()
                    } catch (error: ExecutionException) {
                        Timber.w(error.cause, "Listening service not reachable")
                        null
                    }
                    continuation.resume(result)
                },
                ContextCompat.getMainExecutor(context)
            )
            continuation.invokeOnCancellation { MediaController.releaseFuture(future) }
        } ?: return null
        connected.addListener(listener)
        controller = connected
        return connected
    }

    private fun update(player: Player) {
        // A file the system cleared from the cache, for one; picking the list again writes it anew.
        val error = player.playerError
        if (error != null && error !== lastError) _failures.tryEmit(PlaybackError.FAILED)
        lastError = error
        val previous = _session.value
        val session = sessionOf(player.reading(), queue.source, queue.tracks)
        _session.value = session
        if (session != null && resumePointMoved(previous, session)) resumePoints.trySend(lastPlayedOf(session))
    }
}

private fun Player.reading() = PlayerReading(
    idle = playbackState == Player.STATE_IDLE,
    ended = playbackState == Player.STATE_ENDED,
    itemCount = mediaItemCount,
    index = currentMediaItemIndex,
    playWhenReady = playWhenReady,
    suppressed = playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE
)
