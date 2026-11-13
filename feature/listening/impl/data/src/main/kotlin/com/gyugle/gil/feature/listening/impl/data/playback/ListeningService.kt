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
@file:OptIn(UnstableApi::class)

package com.gyugle.gil.feature.listening.impl.data.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.gyugle.gil.core.domain.ApplicationScope
import com.gyugle.gil.core.domain.onFailure
import com.gyugle.gil.feature.listening.api.LISTENING_DEEP_LINK
import com.gyugle.gil.feature.listening.impl.data.R
import com.gyugle.gil.feature.progress.api.ProgressCommands
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Plays listening mode in the background with its own player, so the app handles audio focus,
 * unplugged headphones and the position, which speech from the engine's process cannot. The system
 * shows the media notification and keeps the service in the foreground while it plays.
 */
@AndroidEntryPoint
class ListeningService : MediaSessionService() {
    @Inject
    internal lateinit var queue: TrackQueue

    @Inject
    internal lateinit var progress: ProgressCommands

    @Inject
    @ApplicationScope
    internal lateinit var scope: CoroutineScope

    private var session: MediaSession? = null
    private lateinit var studyTime: StudyTimeTracker

    override fun onCreate() {
        super.onCreate()
        studyTime = StudyTimeTracker(TimeSource.Monotonic) { duration ->
            // The app scope, so the time is written even as the service goes away.
            scope.launch {
                progress.recordStudyTime(duration).onFailure { Timber.w("Listening time not recorded: %s", it) }
            }
        }
        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_SPEECH).build(),
                // Pauses for a call or another app's sound, and comes back after a short one.
                true
            )
            .setHandleAudioBecomingNoisy(true)
            // Keeps the CPU awake between pieces with the screen off; the foreground service keeps the app.
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setMediaSourceFactory(TrackSourceFactory(queue))
            .build()
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) = studyTime.onPlayingChanged(isPlaying)

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = studyTime.checkpoint()

                // Emptied when the learner stops listening: nothing is left, so the service goes too.
                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (timeline.isEmpty) stopSelf()
                }

                override fun onPlayerError(error: PlaybackException) {
                    Timber.w("Listening playback failed: %s", error.errorCodeName)
                }
            }
        )
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelName(R.string.listening_channel_name)
                .build()
                .apply { setSmallIcon(R.drawable.ic_notification_listening) }
        )
        session = MediaSession.Builder(this, player)
            .setCallback(SessionCallback())
            .apply { openListening()?.let(::setSessionActivity) }
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /**
     * A finished list leaves no notification: Media3 would keep it, and it could not be swiped
     * away while the service stays. The player screen offers to start over, which brings it back.
     *
     * This is the async variant on purpose: once the default `onUpdateNotification` has run, Media3
     * posts its notification after that method returns, so skipping the call there would still bring
     * the notification back, and try to start the service in the foreground from the background.
     */
    override fun onUpdateNotificationAsync(
        session: MediaSession,
        startInForegroundRequired: Boolean
    ): ListenableFuture<Void?> {
        if (session.player.playbackState == Player.STATE_ENDED) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            NotificationManagerCompat.from(this).cancel(DefaultMediaNotificationProvider.DEFAULT_NOTIFICATION_ID)
            return Futures.immediateVoidFuture()
        }
        return super.onUpdateNotificationAsync(session, startInForegroundRequired)
    }

    override fun onDestroy() {
        session?.let { session ->
            studyTime.onPlayingChanged(false)
            session.player.release()
            session.release()
        }
        session = null
        super.onDestroy()
    }

    /** Opens the player screen, from the notification or the lock screen. */
    private fun openListening(): PendingIntent? {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return null
        intent.data = LISTENING_DEEP_LINK.toUri()
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val access = controllerAccess(
                packageName = controller.packageName,
                ownPackageName = packageName,
                trusted = controller.isTrusted,
                notificationController = session.isMediaNotificationController(controller)
            )
            return when (access) {
                ControllerAccess.FULL -> MediaSession.ConnectionResult.AcceptedResultBuilder(session).build()

                ControllerAccess.CONTROL -> MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailablePlayerCommands(
                        MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS.buildUpon()
                            .remove(Player.COMMAND_SET_MEDIA_ITEM)
                            .remove(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                            .build()
                    )
                    .build()

                ControllerAccess.REJECTED -> {
                    Timber.w("Listening session refused a controller")
                    MediaSession.ConnectionResult.reject()
                }
            }
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>
        ): ListenableFuture<MutableList<MediaItem>> {
            val items = mediaItems.mapNotNullTo(mutableListOf()) { queue.playableItem(it.mediaId) }
            return if (items.size == mediaItems.size) {
                Futures.immediateFuture(items)
            } else {
                Futures.immediateFailedFuture(IllegalArgumentException("Unknown listening track"))
            }
        }
    }
}
