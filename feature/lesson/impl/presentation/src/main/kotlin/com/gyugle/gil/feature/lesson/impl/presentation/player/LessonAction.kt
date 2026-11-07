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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import com.gyugle.gil.core.presentation.permissions.PermissionState

sealed interface LessonAction {
    data object OnRetryLoadClick : LessonAction
    data object OnResumeClick : LessonAction
    data object OnRestartClick : LessonAction

    data object OnCloseClick : LessonAction
    data object OnExitConfirm : LessonAction
    data object OnExitDismiss : LessonAction

    data object OnPlayClick : LessonAction
    data object OnPlaySlowlyClick : LessonAction

    /** Plays one sentence of the step, such as an example or a line of a dialogue. */
    data class OnPlaySentenceClick(val text: String) : LessonAction
    data object OnShowMeaningClick : LessonAction

    data class OnOptionClick(val index: Int) : LessonAction
    data class OnTileClick(val bankIndex: Int) : LessonAction
    data class OnChosenTileClick(val position: Int) : LessonAction
    data class OnTextChange(val text: String) : LessonAction
    data object OnCheckClick : LessonAction
    data object OnContinueClick : LessonAction

    data object OnRecordClick : LessonAction
    data object OnStopRecordingClick : LessonAction
    data object OnPlayMineClick : LessonAction
    data object OnAllowMicrophoneClick : LessonAction
    data class OnMicrophoneState(val state: PermissionState) : LessonAction

    data object OnSaveRetryClick : LessonAction
    data object OnNextLessonClick : LessonAction
    data object OnDoneClick : LessonAction

    /** The screen came to the front or left it; time counts only in between. */
    data object OnResumed : LessonAction
    data object OnPaused : LessonAction
}
