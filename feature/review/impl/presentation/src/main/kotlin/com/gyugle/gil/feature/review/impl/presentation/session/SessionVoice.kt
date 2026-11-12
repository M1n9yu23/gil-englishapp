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
package com.gyugle.gil.feature.review.impl.presentation.session

import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.VoiceState
import com.gyugle.gil.feature.settings.api.AppSettings
import com.gyugle.gil.feature.settings.api.SpeechSpeed
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

private val VOICE_WAIT = 3.seconds

/**
 * Whether an English voice is installed. While the engine is still connecting, waits a little:
 * cards made without a voice never play.
 */
internal suspend fun Speaker.waitForEnglishVoice(): Boolean {
    val status = withTimeoutOrNull(VOICE_WAIT) { status.first { it !is SpeakerStatus.Connecting } }
    return status is SpeakerStatus.Connected && status.voice(SpeechLanguage.AMERICAN_ENGLISH) == VoiceState.INSTALLED
}

/** The first play follows the learner's speed; "play slowly" stays slow either way. */
internal val AppSettings.firstPlayRate: SpeechRate
    get() = if (speed == SpeechSpeed.SLOW) SpeechRate.SLOW else SpeechRate.NORMAL
