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

import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.listening.impl.domain.model.ListeningPace
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistSource

/** States for previews and screenshot tests, with content as the first A1 units have it. */
internal object ListeningPreviewData {
    private val recent = SourceUi(PlaylistSource.Recent, SourceName.Recent, 18)
    private val greetings = SourceUi(
        PlaylistSource.CourseUnit(UnitId("unit.a1-01")),
        SourceName.CourseUnit("인사", "Greetings"),
        12
    )

    val choosing = ListeningState.Choosing(
        sources = listOf(
            SourceUi(PlaylistSource.DueToday, SourceName.DueToday, 6),
            recent,
            SourceUi(PlaylistSource.Dialogues, SourceName.Dialogues, 3)
        ),
        units = listOf(
            greetings,
            SourceUi(
                PlaylistSource.CourseUnit(UnitId("unit.a1-02")),
                SourceName.CourseUnit("이름과 나라", "Names and countries"),
                6
            )
        ),
        resume = greetings,
        pace = ListeningPace(),
        koreanVoice = true
    )

    val choosingNoKorean = choosing.copy(resume = null, koreanVoice = false)

    val preparing = ListeningState.Preparing(done = 14, total = 40)

    val playing = ListeningState.Playing(
        track = TrackUi("Nice to meet you.", "만나서 반가워요"),
        position = 3,
        count = 18,
        playing = true
    )

    val paused = playing.copy(playing = false)

    val dialogue = ListeningState.Playing(
        track = TrackUi("Hi, I’m Mina. What’s your name?", "안녕, 나는 미나야. 너는 이름이 뭐야?"),
        position = 1,
        count = 3,
        playing = true
    )

    val ended = ListeningState.Ended(count = 18)
}
