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
package com.gyugle.gil.tools.content.check

import com.gyugle.gil.tools.content.english.Contraction
import com.gyugle.gil.tools.content.english.Contractions
import com.gyugle.gil.tools.content.english.FormKind
import com.gyugle.gil.tools.content.english.MarkedSentence
import com.gyugle.gil.tools.content.english.SenseMark
import com.gyugle.gil.tools.content.english.Token
import com.gyugle.gil.tools.content.english.Tokenizer
import com.gyugle.gil.tools.content.english.normalizeEnglish
import com.gyugle.gil.tools.content.source.PartOfSpeech
import com.gyugle.gil.tools.content.source.SenseSource

/** What the learner has met by some lesson: study item IDs and the literal words of known patterns. */
class Knowledge {
    val items = mutableSetOf<String>()
    val literals = mutableSetOf<String>()
}

/** A word of a sentence linked to the lexicon. Null [word] or [sense] means it could not be decided. */
data class ResolvedToken(val start: Int, val end: Int, val word: String?, val sense: String?)

/**
 * Links each word of a sentence to the lexicon and, given a [Knowledge], reports words the learner
 * has not met yet (the i+1 rule). Without a knowledge it only links words, for text that is not
 * checked.
 *
 * [gates] maps an inflection kind to the item that teaches it. [introducedIn] (item to lesson) and
 * [lessonOrder] only make messages point at the right lesson.
 */
class SentenceResolver(
    private val lexicon: LexiconIndex,
    private val gates: Map<FormKind, String>,
    private val introducedIn: Map<String, String>,
    private val lessonOrder: Map<String, Int>,
    private val issues: Issues
) {

    fun resolve(sentence: MarkedSentence, knowledge: Knowledge?, where: String): List<ResolvedToken> {
        val tokens = Tokenizer.tokenize(sentence.text)
        val result = mutableListOf<ResolvedToken>()
        val usedMarks = mutableSetOf<SenseMark>()
        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            val mark = sentence.senseMarks.firstOrNull { token.start >= it.start && token.end <= it.end }
            val properNoun = sentence.properNouns.firstOrNull { token.start in it }
            when {
                properNoun != null -> {
                    if (token.end > properNoun.last + 1) checkPossessive(token, knowledge, where)
                    i++
                }

                token.isNumber -> i++

                mark != null -> {
                    val covered = tokens.subList(i, tokens.size).takeWhile { it.end <= mark.end }
                    usedMarks += mark
                    resolveMarked(covered, mark, sentence.text, knowledge, where)?.let { sense ->
                        covered.forEach {
                            result +=
                                ResolvedToken(it.start, it.end, lexicon.senseWord.getValue(sense).id, sense)
                        }
                    }
                    i += covered.size
                }

                else -> {
                    val phrase = matchPhrase(tokens, i, sentence, knowledge)
                    if (phrase != null) {
                        val covered = tokens.subList(i, i + phrase.keys.size)
                        val senses = candidateSenses(phrase.word.senses, knowledge)
                        val sense = senses.singleOrNull()?.id
                        if (senses.size > 1) warnAmbiguous(covered.joinToString(" ") { it.text }, senses, where)
                        covered.forEach { result += ResolvedToken(it.start, it.end, phrase.word.id, sense) }
                        i += covered.size
                    } else {
                        result +=
                            resolveHyphenated(token, knowledge, where) ?: listOf(resolveWord(token, knowledge, where))
                        i++
                    }
                }
            }
        }
        (sentence.senseMarks - usedMarks).forEach {
            val shown = sentence.text.substring(it.start, it.end)
            issues.add(Rule.MARKUP_INVALID, where, "the sense mark on '$shown' must cover whole words")
        }
        return result
    }

    /** A known multi-word headword starting at [start] that runs over no marked word or name. */
    private fun matchPhrase(tokens: List<Token>, start: Int, sentence: MarkedSentence, knowledge: Knowledge?): Phrase? =
        lexicon.phrasesStartingWith(tokens[start].key).firstOrNull { phrase ->
            start + phrase.keys.size <= tokens.size &&
                phrase.keys.indices.all { tokens[start + it].key == phrase.keys[it] } &&
                tokens.subList(start, start + phrase.keys.size).none { token ->
                    sentence.senseMarks.any { token.end > it.start && token.start < it.end } ||
                        sentence.properNouns.any { token.start in it }
                } &&
                (knowledge == null || phrase.word.senses.any { it.id in knowledge.items })
        }

    private fun resolveMarked(
        tokens: List<Token>,
        mark: SenseMark,
        text: String,
        knowledge: Knowledge?,
        where: String
    ): String? {
        val shown = text.substring(mark.start, mark.end)
        val sense = lexicon.senses[mark.senseId]
        if (sense == null) {
            issues.add(Rule.REF_MISSING, where, "'$shown' is marked with ${mark.senseId}, which is not in the lexicon")
            return null
        }
        val owner = lexicon.senseWord.getValue(mark.senseId)
        val match = if (tokens.size == 1) {
            lexicon.matches(tokens.single().key).firstOrNull { it.word.id == owner.id && sense in it.senses }
        } else {
            lexicon.phrasesStartingWith(tokens.first().key)
                .firstOrNull { p -> p.word.id == owner.id && p.keys == tokens.map { it.key } }
                ?.let { FormMatch(it.word, null, emptySet()) }
        }
        if (match == null) {
            issues.add(
                Rule.VOCAB_MARK,
                where,
                "'$shown' is not a form of '${owner.word}' (${sense.pos.key}), so it cannot be ${mark.senseId}"
            )
            return null
        }
        if (knowledge != null) {
            if (mark.senseId !in knowledge.items) {
                issues.add(Rule.VOCAB_NOT_YET, where, "'$shown' is ${mark.senseId}, ${taughtWhere(mark.senseId)}")
            } else if (!gateOpen(match.kind, knowledge)) {
                issues.add(Rule.VOCAB_FORM_NOT_YET, where, formMessage(shown, match.kind!!))
            }
        }
        return mark.senseId
    }

    /**
     * A hyphenated word that is not a headword itself: the same word written solid (`co-worker`
     * for `coworker`), or else each part on its own (`twenty-one`). Null when [token] has no
     * hyphen or is a headword.
     */
    private fun resolveHyphenated(token: Token, knowledge: Knowledge?, where: String): List<ResolvedToken>? {
        if ('-' !in token.text || lexicon.matches(token.key).isNotEmpty()) return null
        val solid = token.text.replace("-", "")
        if (lexicon.matches(normalizeEnglish(solid)).isNotEmpty()) {
            val resolved = resolveWord(Token(token.start, token.end, solid), knowledge, where)
            return listOf(resolved)
        }
        var start = token.start
        return token.text.split('-').map { part ->
            val resolved = resolveWord(Token(start, start + part.length, part), knowledge, where)
            start += part.length + 1
            resolved
        }
    }

    private fun resolveWord(token: Token, knowledge: Knowledge?, where: String): ResolvedToken {
        val key = token.key
        val matches = lexicon.matches(key)
        val contraction = if (matches.isEmpty()) Contractions.split(key) else null
        if (contraction != null && lexicon.matches(contraction.base).isNotEmpty()) {
            if (knowledge != null &&
                key !in knowledge.literals
            ) {
                checkContraction(token.text, contraction, knowledge, where)
            }
            return link(token, contraction.base, knowledge)
        }
        if (knowledge != null && key !in knowledge.literals) {
            val known = matches.filter { isKnownMatch(it, knowledge) }
            val senses = known.flatMap { knownSenses(it, knowledge) }.distinct()
            when {
                matches.isEmpty() -> issues.add(Rule.VOCAB_UNKNOWN, where, "'${token.text}' is not in the lexicon")
                known.isEmpty() -> reportUnknown(token.text, matches, knowledge, where)
                senses.size > 1 -> warnAmbiguous(token.text, senses, where)
            }
        }
        return link(token, key, knowledge)
    }

    /** Links [token] to the word and sense spelled [key], without reporting anything. */
    private fun link(token: Token, key: String, knowledge: Knowledge?): ResolvedToken {
        val matches = lexicon.matches(key)
        val known = if (knowledge == null) matches else matches.filter { isKnownMatch(it, knowledge) }
        val senses = if (knowledge == null) {
            matches.flatMap { it.senses }
        } else {
            known.flatMap { knownSenses(it, knowledge) }
        }.distinct()
        val word = known.ifEmpty { matches }.map { it.word.id }.distinct().singleOrNull()
        return ResolvedToken(token.start, token.end, word, senses.singleOrNull()?.id)
    }

    private fun checkContraction(text: String, contraction: Contraction, knowledge: Knowledge, where: String) {
        if (!isKnown(contraction.base, knowledge)) {
            reportUnknown(text, lexicon.matches(contraction.base), knowledge, where)
            return
        }
        // After a word known only as a noun, 's is a possessive (the student's book); after a
        // pronoun or other word it stands for is or has (it's, she's).
        val baseSenses = lexicon.matches(contraction.base).flatMap { knownSenses(it, knowledge) }
        if (contraction.canBePossessive && baseSenses.isNotEmpty() && baseSenses.all { it.pos == PartOfSpeech.NOUN }) {
            if (!gateOpen(FormKind.POSSESSIVE, knowledge)) {
                issues.add(Rule.VOCAB_FORM_NOT_YET, where, formMessage(text, FormKind.POSSESSIVE))
            }
            return
        }
        if (contraction.alternatives.none { isKnown(it, knowledge) }) {
            val needed = contraction.alternatives.joinToString(" or ") { "'$it'" }
            issues.add(Rule.VOCAB_NOT_YET, where, "'$text' needs $needed to be taught first")
        }
    }

    private fun isKnownMatch(match: FormMatch, knowledge: Knowledge): Boolean =
        knownSenses(match, knowledge).isNotEmpty() && gateOpen(match.kind, knowledge)

    private fun isKnown(key: String, knowledge: Knowledge): Boolean =
        key in knowledge.literals || lexicon.matches(key).any { isKnownMatch(it, knowledge) }

    private fun reportUnknown(text: String, matches: List<FormMatch>, knowledge: Knowledge, where: String) {
        val gated = matches.firstOrNull { knownSenses(it, knowledge).isNotEmpty() && !gateOpen(it.kind, knowledge) }
        if (gated != null) {
            issues.add(Rule.VOCAB_FORM_NOT_YET, where, formMessage(text, gated.kind!!))
            return
        }
        val firstSense = matches.flatMap { it.senses }.mapNotNull { s -> introducedIn[s.id]?.let { s.id to it } }
            .minByOrNull { lessonOrder[it.second] ?: Int.MAX_VALUE }
        val message = if (firstSense != null) {
            "'$text' is not taught yet: ${firstSense.first} comes in ${firstSense.second}"
        } else {
            "'$text' is not taught yet: no lesson introduces a sense of it"
        }
        issues.add(Rule.VOCAB_NOT_YET, where, message)
    }

    private fun checkPossessive(token: Token, knowledge: Knowledge?, where: String) {
        if (knowledge != null && !gateOpen(FormKind.POSSESSIVE, knowledge)) {
            issues.add(Rule.VOCAB_FORM_NOT_YET, where, formMessage(token.text, FormKind.POSSESSIVE))
        }
    }

    private fun knownSenses(match: FormMatch, knowledge: Knowledge): List<SenseSource> =
        match.senses.filter { it.id in knowledge.items }

    private fun candidateSenses(senses: List<SenseSource>, knowledge: Knowledge?): List<SenseSource> =
        if (knowledge == null) senses else senses.filter { it.id in knowledge.items }

    private fun gateOpen(kind: FormKind?, knowledge: Knowledge): Boolean {
        val gate = kind?.let { gates[it] } ?: return true
        return gate in knowledge.items
    }

    private fun formMessage(text: String, kind: FormKind): String {
        val gate = gates.getValue(kind)
        return "'$text' is a ${kind.key} form, which is taught with $gate in ${introducedIn[gate] ?: "no lesson"}"
    }

    private fun taughtWhere(id: String): String =
        introducedIn[id]?.let { "which is not taught until $it" } ?: "which no lesson introduces"

    private fun warnAmbiguous(text: String, senses: List<SenseSource>, where: String) {
        issues.add(
            Rule.SENSE_AMBIGUOUS,
            where,
            "'$text' could be ${senses.joinToString { it.id }}; mark the one meant, like {$text|${senses.first().id}}"
        )
    }
}
