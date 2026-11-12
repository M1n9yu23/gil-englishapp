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
package com.gyugle.gil.feature.review.impl.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.first
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.review.impl.domain.model.CardKind
import org.junit.jupiter.api.Test

class CardKindsTest {
    private val three = SenseId("sense.three.num.count")
    private val thanks = PatternId("pattern.thank-you")

    private fun card(id: StudyItemId, stability: Double, reviews: Int = 1) = CardState(
        id,
        stability,
        dueDay = StudyDay(10),
        lastReviewDay = StudyDay(5),
        reviewCount = reviews,
        lapseCount = 0
    )

    @Test
    fun cardKinds_newItem_recognisesTheMeaningFirst() {
        assertThat(cardKinds(card(three, stability = 2.0), hasVoice = true)).first().isEqualTo(CardKind.CHOOSE_MEANING)
    }

    @Test
    fun cardKinds_middleStability_takesTurnsBetweenEarAndMeaning() {
        assertThat(cardKinds(card(three, 8.0, reviews = 2), hasVoice = true))
            .first().isEqualTo(CardKind.LISTEN_CHOOSE)
        assertThat(cardKinds(card(three, 8.0, reviews = 3), hasVoice = true))
            .first().isEqualTo(CardKind.CHOOSE_ENGLISH)
    }

    @Test
    fun cardKinds_noVoice_neverListens() {
        val kinds = cardKinds(card(three, 8.0, reviews = 2), hasVoice = false)

        assertThat(kinds).containsExactly(CardKind.CHOOSE_ENGLISH, CardKind.CHOOSE_MEANING, CardKind.SPELL)
    }

    @Test
    fun cardKinds_wellKnownWord_isProducedThenFallsBack() {
        assertThat(cardKinds(card(three, 30.0, reviews = 4), hasVoice = true)).containsExactly(
            CardKind.FILL_BLANK,
            CardKind.SPELL,
            CardKind.CHOOSE_ENGLISH,
            CardKind.CHOOSE_MEANING
        )
        assertThat(cardKinds(card(three, 30.0, reviews = 5), hasVoice = true))
            .first().isEqualTo(CardKind.SPELL)
    }

    @Test
    fun cardKinds_wellKnownPattern_isRecalledNotSpelledFirst() {
        assertThat(cardKinds(card(thanks, 30.0), hasVoice = true))
            .containsExactly(CardKind.CHOOSE_ENGLISH, CardKind.CHOOSE_MEANING, CardKind.SPELL)
    }

    @Test
    fun cardKinds_sounds_letterByNameOtherSoundsByWord() {
        assertThat(cardKinds(card(SoundId("sound.letter-a"), 30.0), hasVoice = false))
            .containsExactly(CardKind.CHOOSE_LETTER)
        assertThat(cardKinds(card(SoundId("sound.f"), 1.0), hasVoice = true))
            .containsExactly(CardKind.FIND_SOUND)
    }

    @Test
    fun testCardKinds_word_alternatesEarAndMeaningThenFallsBack() {
        assertThat(testCardKinds(three, number = 0, hasVoice = true)).containsExactly(
            CardKind.LISTEN_CHOOSE,
            CardKind.CHOOSE_ENGLISH,
            CardKind.CHOOSE_MEANING,
            CardKind.SPELL
        )
        assertThat(testCardKinds(thanks, number = 1, hasVoice = true))
            .containsExactly(CardKind.CHOOSE_ENGLISH, CardKind.CHOOSE_MEANING, CardKind.SPELL)
    }

    @Test
    fun testCardKinds_noVoice_neverListens() {
        assertThat(testCardKinds(three, number = 0, hasVoice = false))
            .containsExactly(CardKind.CHOOSE_ENGLISH, CardKind.CHOOSE_MEANING, CardKind.SPELL)
    }

    @Test
    fun testCardKinds_letterAndSound_askedAsInAReview() {
        assertThat(testCardKinds(SoundId("sound.letter-b"), number = 0, hasVoice = true))
            .containsExactly(CardKind.CHOOSE_LETTER)
        assertThat(testCardKinds(SoundId("sound.th-voiceless"), number = 1, hasVoice = true))
            .containsExactly(CardKind.FIND_SOUND)
    }
}
