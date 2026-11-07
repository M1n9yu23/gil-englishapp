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
package com.gyugle.gil.core.speech.di

import com.gyugle.gil.core.speech.AndroidOnDeviceRecognizer
import com.gyugle.gil.core.speech.AndroidTextToSpeechSpeaker
import com.gyugle.gil.core.speech.AudioRecordVoiceRecorder
import com.gyugle.gil.core.speech.OnDeviceRecognizer
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.VoiceRecorder
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface SpeechModule {
    @Binds
    fun speaker(speaker: AndroidTextToSpeechSpeaker): Speaker

    @Binds
    fun voiceRecorder(recorder: AudioRecordVoiceRecorder): VoiceRecorder

    @Binds
    fun onDeviceRecognizer(recognizer: AndroidOnDeviceRecognizer): OnDeviceRecognizer
}
