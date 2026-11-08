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
package com.gyugle.gil.feature.listening.impl.data

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.core.speech.Speaker
import com.gyugle.gil.core.speech.SpeakerStatus
import com.gyugle.gil.core.speech.SpeechError
import com.gyugle.gil.core.speech.SpeechLanguage
import com.gyugle.gil.core.speech.SpeechRate
import com.gyugle.gil.core.speech.englishVoice
import com.gyugle.gil.feature.listening.impl.domain.TrackRenderer
import com.gyugle.gil.feature.listening.impl.domain.model.Clip
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningSpeed
import com.gyugle.gil.feature.listening.impl.domain.model.RenderError
import com.gyugle.gil.feature.listening.impl.domain.model.RenderProgress
import com.gyugle.gil.feature.listening.impl.domain.model.RenderedTrack
import com.gyugle.gil.feature.listening.impl.domain.model.SpokenLanguage
import com.gyugle.gil.feature.listening.impl.domain.model.Track
import com.gyugle.gil.feature.listening.impl.domain.model.TrackPart
import com.gyugle.gil.feature.settings.api.Accent
import com.gyugle.gil.feature.settings.api.AppSettingsObserver
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

/**
 * Writes speech with the phone's speech engine into [SpeechCache], one file per distinct piece:
 * an example heard twice is written once.
 */
internal class TtsTrackRenderer @Inject constructor(
    private val speaker: Speaker,
    private val settings: AppSettingsObserver,
    private val cache: SpeechCache
) : TrackRenderer {

    override fun render(tracks: List<Track>, speed: ListeningSpeed): Flow<RenderProgress> = flow {
        val status = speaker.status.first { it !is SpeakerStatus.Connecting }
        if (status !is SpeakerStatus.Connected) {
            emit(RenderProgress.Failed(RenderError.NO_ENGINE))
            return@flow
        }
        val english = status.englishVoice(preferBritish = settings.observeSettings().first().accent == Accent.BRITISH)
        fun keyOf(speech: TrackPart.Speech) = when (speech.language) {
            SpokenLanguage.ENGLISH -> SpeechKey(
                speech.text,
                english,
                if (speed == ListeningSpeed.SLOW) SpeechRate.SLOW else SpeechRate.NORMAL
            )

            SpokenLanguage.KOREAN -> SpeechKey(speech.text, SpeechLanguage.KOREAN, SpeechRate.NORMAL)
        }

        val keys = tracks.flatMap { it.parts }.filterIsInstance<TrackPart.Speech>().map(::keyOf).distinct()
        val files = mutableMapOf<SpeechKey, Clip.Audio>()
        cache.prepare()
        emit(RenderProgress.Writing(done = 0, total = keys.size))
        keys.forEachIndexed { index, key ->
            val file = cache.fileFor(key)
            if (!cache.isReady(file)) {
                val written = speaker.synthesizeToFile(key.text, key.language, key.rate, file)
                if (written is Outcome.Failure) {
                    emit(RenderProgress.Failed(written.error.toRenderError()))
                    return@flow
                }
            }
            files[key] = Clip.Audio(file.path, cache.durationOf(file))
            emit(RenderProgress.Writing(done = index + 1, total = keys.size))
        }
        cache.trim(keep = files.values.mapTo(mutableSetOf()) { File(it.path) })
        emit(
            RenderProgress.Done(
                tracks.map { track ->
                    RenderedTrack(
                        id = track.id,
                        title = track.title,
                        subtitle = track.subtitle,
                        clips = track.parts.map { part ->
                            when (part) {
                                is TrackPart.Speech -> files.getValue(keyOf(part))
                                is TrackPart.Pause -> Clip.Silence(part.duration)
                            }
                        }
                    )
                }
            )
        )
    }
}

private fun SpeechError.toRenderError(): RenderError = when (this) {
    SpeechError.NO_ENGINE -> RenderError.NO_ENGINE
    SpeechError.VOICE_NOT_INSTALLED -> RenderError.VOICE_NOT_INSTALLED
    SpeechError.FAILED -> RenderError.FAILED
}
