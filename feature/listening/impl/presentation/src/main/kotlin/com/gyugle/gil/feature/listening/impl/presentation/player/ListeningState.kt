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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.PauseLength
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource
import com.gyugle.gil.feature.listening.impl.presentation.R

@Immutable
sealed interface ListeningState {
    data object Loading : ListeningState

    /** The phone has no speech engine. */
    data object NoEngine : ListeningState

    /** No English voice is installed; without one there is nothing to play. */
    data object NoEnglishVoice : ListeningState

    /** The lessons or the record could not be read. */
    data object Failed : ListeningState

    /** No finished lesson has anything to hear. */
    data object NothingYet : ListeningState

    /**
     * Picking what to hear. [resume] is where the learner stopped last time, when that source still
     * has something. Without [koreanVoice] the meanings are left out and the screen says so.
     */
    data class Choosing(
        val sources: List<SourceUi>,
        val units: List<SourceUi>,
        val resume: SourceUi?,
        val pace: ListeningPace,
        val koreanVoice: Boolean,
        val dialog: PaceDialog? = null
    ) : ListeningState

    /** Writing the audio: [done] of [total] pieces. */
    data class Preparing(val done: Int, val total: Int) : ListeningState

    /** A playlist is loaded; [playing] is false while paused. */
    data class Playing(val track: TrackUi, val position: Int, val count: Int, val playing: Boolean) : ListeningState

    data class Ended(val count: Int) : ListeningState
}

/** A source to pick, with its name and how much it holds. */
@Immutable
data class SourceUi(val source: PlaylistSource, val name: SourceName, val count: Int)

@Immutable
sealed interface SourceName {
    data object DueToday : SourceName

    data object Recent : SourceName

    data object Dialogues : SourceName

    /** A unit's title from the content, in both app languages. */
    data class CourseUnit(val ko: String, val en: String) : SourceName

    @Composable
    fun asString(): String = when (this) {
        DueToday -> stringResource(R.string.listening_source_due)
        Recent -> stringResource(R.string.listening_source_recent)
        Dialogues -> stringResource(R.string.listening_source_dialogues)
        is CourseUnit -> if (stringResource(R.string.listening_content_language) == "ko") ko else en
    }
}

internal fun LocalizedText.toSourceName(): SourceName = SourceName.CourseUnit(ko = ko, en = en)

/** [title] is English; [subtitle] is its Korean, when there is one. */
@Immutable
data class TrackUi(val title: String, val subtitle: String?)

enum class PaceDialog {
    SPEED,
    PAUSE
}

sealed interface ListeningAction {
    data class OnSourceClick(val source: PlaylistSource) : ListeningAction

    data object OnResumeClick : ListeningAction

    data object OnSpeedClick : ListeningAction

    data object OnPauseLengthClick : ListeningAction

    data class OnSpeedPick(val speed: ListeningSpeed) : ListeningAction

    data class OnPauseLengthPick(val pause: PauseLength) : ListeningAction

    data object OnDialogDismiss : ListeningAction

    data object OnInstallClick : ListeningAction

    /** No app answered the install request. */
    data object OnInstallUnavailable : ListeningAction

    data object OnCancelPreparingClick : ListeningAction

    data object OnPlayPauseClick : ListeningAction

    data object OnPreviousClick : ListeningAction

    data object OnNextClick : ListeningAction

    data object OnStopClick : ListeningAction

    data object OnPlayAgainClick : ListeningAction

    data object OnRetryClick : ListeningAction

    /** Back on the screen, perhaps from installing voices or after the record changed. */
    data object OnResumed : ListeningAction
}

sealed interface ListeningEvent {
    data object OpenVoiceInstall : ListeningEvent

    data class ShowMessage(val message: UiText) : ListeningEvent
}
