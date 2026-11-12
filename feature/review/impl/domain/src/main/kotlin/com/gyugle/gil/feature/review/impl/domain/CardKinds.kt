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

import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.review.impl.domain.model.CardKind

/** Below this stability an item is still new, so the card only asks to recognise it. */
internal const val RECOGNISE_BELOW_DAYS = 4.0

/** From this stability a word is known well enough to be produced: spelled or filled in. */
internal const val PRODUCE_FROM_DAYS = 15.0

/**
 * The kinds of card to ask about [card]'s item, the preferred first. A kind falls through to the
 * next when the content cannot make it, such as a blank for a word with no example sentence.
 *
 * A word or pattern moves from recognising (its meaning) to recalling (the English, from its
 * meaning or by ear) to producing (spelling, a blank) as its stability grows. Kinds of one level
 * take turns from one review to the next. A letter is always picked by its name, a sound by a
 * word that has it.
 */
fun cardKinds(card: CardState, hasVoice: Boolean): List<CardKind> {
    val id = card.itemId
    if (id is SoundId) return listOf(if (id.isLetter) CardKind.CHOOSE_LETTER else CardKind.FIND_SOUND)
    val even = card.reviewCount % 2 == 0
    val preferred = when {
        card.stabilityDays < RECOGNISE_BELOW_DAYS -> listOf(CardKind.CHOOSE_MEANING)

        card.stabilityDays < PRODUCE_FROM_DAYS -> when {
            !hasVoice -> listOf(CardKind.CHOOSE_ENGLISH)
            even -> listOf(CardKind.LISTEN_CHOOSE, CardKind.CHOOSE_ENGLISH)
            else -> listOf(CardKind.CHOOSE_ENGLISH, CardKind.LISTEN_CHOOSE)
        }

        id is SenseId && even -> listOf(CardKind.FILL_BLANK, CardKind.SPELL)

        id is SenseId -> listOf(CardKind.SPELL, CardKind.FILL_BLANK)

        else -> listOf(CardKind.CHOOSE_ENGLISH)
    }
    // A word or pattern can always be asked by its meaning, or else spelled.
    val fallbacks = listOf(CardKind.CHOOSE_ENGLISH, CardKind.CHOOSE_MEANING, CardKind.SPELL)
    return (preferred + fallbacks).distinct()
}

/**
 * The kinds of card to ask about [itemId] as question [number] of a stage test, the preferred
 * first. A test checks that the learner can recall a word or pattern, not only recognise it: from
 * its meaning, and by ear on every other question when there is a voice. Letters and sounds are
 * asked as in a review.
 */
fun testCardKinds(itemId: StudyItemId, number: Int, hasVoice: Boolean): List<CardKind> {
    if (itemId is SoundId) return listOf(if (itemId.isLetter) CardKind.CHOOSE_LETTER else CardKind.FIND_SOUND)
    val byEar = hasVoice && number % 2 == 0
    val preferred = if (byEar) CardKind.LISTEN_CHOOSE else CardKind.CHOOSE_ENGLISH
    return listOf(preferred, CardKind.CHOOSE_ENGLISH, CardKind.CHOOSE_MEANING, CardKind.SPELL).distinct()
}

internal val SoundId.isLetter: Boolean get() = value.startsWith(LETTER_PREFIX)

private const val LETTER_PREFIX = "sound.letter-"
