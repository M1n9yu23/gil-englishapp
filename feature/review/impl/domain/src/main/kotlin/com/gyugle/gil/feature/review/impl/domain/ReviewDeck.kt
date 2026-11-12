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

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LexiconObserver
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.SoundEntry
import com.gyugle.gil.feature.curriculum.api.SoundExample
import com.gyugle.gil.feature.curriculum.api.SoundId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.review.impl.domain.model.CardKind
import com.gyugle.gil.feature.review.impl.domain.model.ReviewCard
import javax.inject.Inject
import kotlin.random.Random

/** Up to this many wrong options next to the right one. */
internal const val MAX_DISTRACTORS = 3

/**
 * Makes review cards from the dictionary. Wrong options come from the learner's own items, so a
 * card never offers a word the learner has not met; letters, which every learner knows the set
 * of, are the exception and fill up from the alphabet.
 */
class ReviewDeck @Inject constructor(private val lexicon: LexiconObserver) {

    /**
     * A card for [card]'s item, of the first kind in [cardKinds] the content can make, with options
     * placed by [random]. [pool] holds every item the learner has met, including this one.
     *
     * Null when no kind can be made, such as for an item the content no longer has; the session
     * then passes it over. Fails only when the dictionary cannot be read.
     */
    suspend fun cardFor(
        card: CardState,
        pool: List<LessonItem>,
        hasVoice: Boolean,
        random: Random
    ): Outcome<ReviewCard?, ContentError> {
        val item = pool.firstOrNull { it.id == card.itemId } ?: return Outcome.Success(null)
        return cardFor(item, cardKinds(card, hasVoice), pool, hasVoice, random)
    }

    /**
     * A card for [item], of the first of [kinds] the content can make. Otherwise as the other
     * [cardFor], for an item that may have no card yet, such as in a stage test.
     */
    suspend fun cardFor(
        item: LessonItem,
        kinds: List<CardKind>,
        pool: List<LessonItem>,
        hasVoice: Boolean,
        random: Random
    ): Outcome<ReviewCard?, ContentError> {
        for (kind in kinds) {
            when (val made = make(kind, item, pool, hasVoice, random)) {
                is Outcome.Failure -> return made
                is Outcome.Success -> made.data?.let { return Outcome.Success(it) }
            }
        }
        return Outcome.Success(null)
    }

    private suspend fun make(
        kind: CardKind,
        item: LessonItem,
        pool: List<LessonItem>,
        hasVoice: Boolean,
        random: Random
    ): Outcome<ReviewCard?, ContentError> = when (kind) {
        CardKind.CHOOSE_MEANING -> Outcome.Success(chooseMeaning(item, pool, hasVoice, random))
        CardKind.CHOOSE_ENGLISH -> Outcome.Success(chooseEnglish(item, pool, random))
        CardKind.LISTEN_CHOOSE -> Outcome.Success(if (hasVoice) listenChoose(item, pool, random) else null)
        CardKind.CHOOSE_LETTER -> Outcome.Success(chooseLetter(item, pool, random))
        CardKind.SPELL -> Outcome.Success(spell(item, hasVoice))
        CardKind.FIND_SOUND -> findSound(item, pool, random)
        CardKind.FILL_BLANK -> fillBlank(item, random)
    }

    private fun chooseMeaning(
        item: LessonItem,
        pool: List<LessonItem>,
        hasVoice: Boolean,
        random: Random
    ): ReviewCard? {
        val meaning = item.meaningKo ?: return null
        val wrong = pool.othersLike(item, random)
            .mapNotNull { it.meaningKo }
            .filter { it != meaning }
            .distinct()
            .take(MAX_DISTRACTORS)
        if (wrong.isEmpty()) return null
        val options = (wrong + meaning).shuffled(random)
        return ReviewCard.ChooseMeaning(
            itemId = item.id,
            english = item.text,
            ipa = item.ipa,
            speakable = hasVoice && item.text.isSpeakable(),
            options = options,
            answer = options.indexOf(meaning)
        )
    }

    private fun chooseEnglish(item: LessonItem, pool: List<LessonItem>, random: Random): ReviewCard? {
        val meaning = item.meaningKo ?: return null
        // Two items with one meaning, such as hi and hello, would make two right answers.
        val wrong = englishDistractors(item, pool.filter { it.meaningKo != meaning }, random)
        if (wrong.isEmpty()) return null
        val options = (wrong + item.text).shuffled(random)
        return ReviewCard.ChooseEnglish(item.id, meaning, options, options.indexOf(item.text))
    }

    private fun listenChoose(item: LessonItem, pool: List<LessonItem>, random: Random): ReviewCard? {
        if (!item.text.isSpeakable()) return null
        val wrong = englishDistractors(item, pool.filter { it.text.isSpeakable() }, random)
        if (wrong.isEmpty()) return null
        val options = (wrong + item.text).shuffled(random)
        return ReviewCard.ListenChoose(item.id, item.text, item.meaningKo, options, options.indexOf(item.text))
    }

    private fun englishDistractors(item: LessonItem, pool: List<LessonItem>, random: Random): List<String> =
        pool.othersLike(item, random)
            .map { it.text }
            .filter { !it.equals(item.text, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .take(MAX_DISTRACTORS)

    private fun chooseLetter(item: LessonItem, pool: List<LessonItem>, random: Random): ReviewCard? {
        val id = item.id as? SoundId ?: return null
        if (!id.isLetter) return null
        val learned = pool.filter { (it.id as? SoundId)?.isLetter == true }.map { it.text }
        val wrong = (learned.shuffled(random) + ALPHABET.shuffled(random))
            .filter { it != item.text }
            .distinct()
            .take(MAX_DISTRACTORS)
        val options = (wrong + item.text).shuffled(random)
        return ReviewCard.ChooseLetter(
            itemId = id,
            // A capital letter on its own is read by its name, as in the lessons.
            spoken = item.text.substringBefore(' '),
            nameIpa = item.ipa.orEmpty(),
            options = options,
            answer = options.indexOf(item.text)
        )
    }

    private fun spell(item: LessonItem, hasVoice: Boolean): ReviewCard? {
        val meaning = item.meaningKo ?: return null
        if (!item.text.isSpeakable()) return null
        return ReviewCard.Spell(item.id, item.text, meaning, item.ipa, speakable = hasVoice)
    }

    /**
     * One example of the sound against words of other sounds the learner knows that do not have
     * the sound, judged by their IPA. Letters count too: their examples are ordinary words, and
     * the course teaches every letter before the first sound, so a sound learned alone still has
     * wrong options.
     */
    private suspend fun findSound(
        item: LessonItem,
        pool: List<LessonItem>,
        random: Random
    ): Outcome<ReviewCard?, ContentError> {
        val id = item.id as? SoundId ?: return Outcome.Success(null)
        val entry = when (val read = soundEntry(id)) {
            is Outcome.Failure -> return read
            is Outcome.Success -> read.data ?: return Outcome.Success(null)
        }
        val right = entry.examples.randomOrNull(random) ?: return Outcome.Success(null)
        val ownWords = entry.examples.map { it.text }.toSet()
        val others = pool.mapNotNull { other -> (other.id as? SoundId)?.takeIf { it != id } }
            .shuffled(random)
            .sortedBy { if (it.isLetter) 1 else 0 }
        val wrong = mutableListOf<SoundExample>()
        for (other in others) {
            if (wrong.size == MAX_DISTRACTORS) break
            val otherEntry = when (val read = soundEntry(other)) {
                is Outcome.Failure -> return read
                is Outcome.Success -> read.data ?: continue
            }
            otherEntry.examples
                .filter { example ->
                    example.text !in ownWords &&
                        !example.ipa.contains(entry.symbol) &&
                        wrong.none { it.text == example.text }
                }
                .randomOrNull(random)
                ?.let { wrong += it }
        }
        if (wrong.isEmpty()) return Outcome.Success(null)
        val options = (wrong + right).shuffled(random)
        return Outcome.Success(
            ReviewCard.FindSound(id, entry.symbol, entry.descriptionKo, options, options.indexOf(right))
        )
    }

    private suspend fun soundEntry(id: SoundId): Outcome<SoundEntry?, ContentError> =
        when (val read = lexicon.getEntry(id)) {
            is Outcome.Success -> Outcome.Success(read.data as? SoundEntry)
            is Outcome.Failure -> read.orNullWhenMissing()
        }

    /**
     * An example sentence of the sense with the words that carry it left out. A sentence that is
     * only the word itself teaches nothing as a blank, so it is passed over.
     */
    private suspend fun fillBlank(item: LessonItem, random: Random): Outcome<ReviewCard?, ContentError> {
        val id = item.id as? SenseId ?: return Outcome.Success(null)
        val meaning = item.meaningKo ?: return Outcome.Success(null)
        val wordId = when (val read = lexicon.getWordOf(id)) {
            is Outcome.Failure -> return read.orNullWhenMissing()
            is Outcome.Success -> read.data
        }
        val word = when (val read = lexicon.getEntry(wordId)) {
            is Outcome.Failure -> return read.orNullWhenMissing()
            is Outcome.Success -> read.data as? WordEntry ?: return Outcome.Success(null)
        }
        val examples = word.senses.firstOrNull { it.id == id }?.examples.orEmpty().shuffled(random)
        for (example in examples) {
            val breakdown = when (val read = lexicon.getSentence(example.id)) {
                is Outcome.Failure -> if (read.error == ContentError.NOT_FOUND) continue else return read
                is Outcome.Success -> read.data
            }
            val words = breakdown.words.filter { it.sense?.id == id }.sortedBy { it.start }
            if (words.isEmpty()) continue
            val text = breakdown.sentence.text
            // Words of one sense apart, as in "pick it up", would blank the words between them too.
            val apart = words.zipWithNext().any { (a, b) -> text.substring(a.end, b.start).isNotBlank() }
            if (apart) continue
            val start = words.first().start
            val end = words.last().end
            val rest = text.removeRange(start, end)
            if (rest.none { it.isLetterOrDigit() }) continue
            return Outcome.Success(
                ReviewCard.FillBlank(id, text, start, end, breakdown.sentence.ko, meaning)
            )
        }
        return Outcome.Success(null)
    }

    private fun Outcome.Failure<ContentError>.orNullWhenMissing(): Outcome<Nothing?, ContentError> =
        if (error == ContentError.NOT_FOUND) Outcome.Success(null) else this
}

/**
 * The other items in random order, those of the same kind as [item] first: a word's wrong options
 * are other words, a pattern's other patterns, as far as the learner has them.
 */
private fun List<LessonItem>.othersLike(item: LessonItem, random: Random): List<LessonItem> =
    filter { it.id != item.id }
        .shuffled(random)
        .sortedBy { if (it.id::class == item.id::class) 0 else 1 }

/** A pattern written with a placeholder, such as "~s", cannot be read aloud or typed. */
private fun String.isSpeakable(): Boolean = PLACEHOLDER !in this

private const val PLACEHOLDER = '~'

/** Every letter as a letter item spells it, capital and small. */
private val ALPHABET = ('A'..'Z').map { "$it ${it.lowercaseChar()}" }
