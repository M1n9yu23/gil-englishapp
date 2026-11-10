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

import androidx.compose.runtime.Immutable

/**
 * Whether the learner may record, and what happened to the last recording.
 *
 * @property score what the phone's recognizer made of the last recording; null when it does not check
 * @property notice why recordings are not checked, shown once ever
 */
@Immutable
data class RecorderUi(
    val microphone: MicrophoneUi,
    val isRecording: Boolean = false,
    val hasRecording: Boolean = false,
    val failed: Boolean = false,
    val score: ScoreUi? = null,
    val notice: ScoringNoticeUi? = null
)
