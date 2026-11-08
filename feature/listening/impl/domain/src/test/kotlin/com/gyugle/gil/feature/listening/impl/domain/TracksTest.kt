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
package com.gyugle.gil.feature.listening.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.DialogueLine
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.listening.impl.domain.model.PauseLength
import com.gyugle.gil.feature.listening.impl.domain.model.PlaylistEntry
import com.gyugle.gil.feature.listening.impl.domain.model.SpokenLanguage.ENGLISH
import com.gyugle.gil.feature.listening.impl.domain.model.SpokenLanguage.KOREAN
import com.gyugle.gil.feature.listening.impl.domain.model.Track
import com.gyugle.gil.feature.listening.impl.domain.model.TrackPart
import org.junit.jupiter.api.Test

class TracksTest {
    private val pause = PauseLength.LONG
    private val item = PlaylistEntry.Item(
        id = PatternId("pattern.nice-to-meet-you"),
        english = "Nice to meet you.",
        korean = "만나서 반가워요",
        examples = listOf(
            Sentence(1, "Hi, I’m Mina. Nice to meet you.", "안녕하세요, 저는 미나예요. 만나서 반가워요."),
            Sentence(2, "Nice to meet you too.", "저도 만나서 반가워요.")
        )
    )

    @Test
    fun trackOf_item_saysEnglishWaitsSaysKoreanThenFirstExampleTwice() {
        val track = trackOf(item, pause, withKorean = true)

        assertThat(track).isEqualTo(
            Track(
                id = "pattern.nice-to-meet-you",
                title = "Nice to meet you.",
                subtitle = "만나서 반가워요",
                parts = listOf(
                    TrackPart.Speech("Nice to meet you.", ENGLISH),
                    TrackPart.Pause(pause.duration),
                    TrackPart.Speech("만나서 반가워요", KOREAN),
                    TrackPart.Pause(GAP),
                    TrackPart.Speech("Hi, I’m Mina. Nice to meet you.", ENGLISH),
                    TrackPart.Pause(GAP),
                    TrackPart.Speech("Hi, I’m Mina. Nice to meet you.", ENGLISH),
                    TrackPart.Pause(GAP)
                )
            )
        )
    }

    @Test
    fun trackOf_itemWithoutKoreanVoice_leavesKoreanOutAndKeepsThePause() {
        val track = trackOf(item, pause, withKorean = false)

        assertThat(track.parts.take(3)).isEqualTo(
            listOf(
                TrackPart.Speech("Nice to meet you.", ENGLISH),
                TrackPart.Pause(pause.duration),
                TrackPart.Speech("Hi, I’m Mina. Nice to meet you.", ENGLISH)
            )
        )
        assertThat(track.parts.none { it is TrackPart.Speech && it.language == KOREAN }).isEqualTo(true)
    }

    @Test
    fun trackOf_itemWithoutExample_repeatsItsEnglish() {
        val track = trackOf(item.copy(examples = emptyList()), pause, withKorean = true)

        assertThat(track.parts.drop(4)).isEqualTo(
            listOf(
                TrackPart.Speech("Nice to meet you.", ENGLISH),
                TrackPart.Pause(GAP),
                TrackPart.Speech("Nice to meet you.", ENGLISH),
                TrackPart.Pause(GAP)
            )
        )
    }

    @Test
    fun trackOf_patternWithASlot_leavesTheSlotSilentButShowsIt() {
        val pattern = item.copy(english = "Call me ~.", korean = "~라고 불러 주세요", examples = emptyList())

        val track = trackOf(pattern, pause, withKorean = true)

        assertThat(track.title).isEqualTo("Call me ~.")
        assertThat(track.parts[0]).isEqualTo(TrackPart.Speech("Call me.", ENGLISH))
        assertThat(track.parts[2]).isEqualTo(TrackPart.Speech("라고 불러 주세요", KOREAN))
    }

    @Test
    fun trackOf_dialogue_saysEachLineWithItsKoreanThenTheWholeDialogueInEnglish() {
        val dialogue = PlaylistEntry.Dialogue(
            lessonId = LessonId("lesson.a1-01.2"),
            index = 1,
            lines = listOf(
                DialogueLine("Mina", Sentence(1, "Hi, I’m Mina.", "안녕, 나는 미나야.")),
                DialogueLine("Tom", Sentence(2, "I’m Tom.", null))
            )
        )

        val track = trackOf(dialogue, PauseLength.SHORT, withKorean = true)

        assertThat(track).isEqualTo(
            Track(
                id = "lesson.a1-01.2#1",
                title = "Hi, I’m Mina.",
                subtitle = "안녕, 나는 미나야.",
                parts = listOf(
                    TrackPart.Speech("Hi, I’m Mina.", ENGLISH),
                    TrackPart.Pause(PauseLength.SHORT.duration),
                    TrackPart.Speech("안녕, 나는 미나야.", KOREAN),
                    TrackPart.Pause(GAP),
                    // No translation in the content: only the pause.
                    TrackPart.Speech("I’m Tom.", ENGLISH),
                    TrackPart.Pause(PauseLength.SHORT.duration),
                    TrackPart.Speech("Hi, I’m Mina.", ENGLISH),
                    TrackPart.Pause(GAP),
                    TrackPart.Speech("I’m Tom.", ENGLISH),
                    TrackPart.Pause(GAP)
                )
            )
        )
    }
}
