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

import com.gyugle.gil.tools.content.english.FormKind
import com.gyugle.gil.tools.content.english.Inflector
import com.gyugle.gil.tools.content.english.Tokenizer
import com.gyugle.gil.tools.content.english.normalizeEnglish
import com.gyugle.gil.tools.content.source.FormsSource
import com.gyugle.gil.tools.content.source.PartOfSpeech
import com.gyugle.gil.tools.content.source.SenseSource
import com.gyugle.gil.tools.content.source.WordSource

/** A spelling that belongs to [word]. [kind] is null for the headword itself. */
data class FormMatch(val word: WordSource, val kind: FormKind?, val pos: Set<PartOfSpeech>) {
    /** Senses of [word] this spelling can stand for. */
    val senses: List<SenseSource> get() = word.senses.filter { kind == null || it.pos in pos }
}

/** A headword of more than one word, such as `next to`, as lowercase keys. */
data class Phrase(val word: WordSource, val keys: List<String>)

/** Lookup from every spelling of every word (headword, irregular and regular forms) to the word. */
class LexiconIndex(words: List<WordSource>, private val inflector: Inflector) {

    val words: Map<String, WordSource> = words.associateBy { it.id }
    val senses: Map<String, SenseSource> = words.flatMap { it.senses }.associateBy { it.id }
    val senseWord: Map<String, WordSource> = words.flatMap { w -> w.senses.map { it.id to w } }.toMap()

    private val forms = mutableMapOf<String, MutableList<FormMatch>>()
    private val phrases = mutableMapOf<String, MutableList<Phrase>>()
    private val allForms = mutableMapOf<String, Map<FormKind, String>>()

    init {
        for (word in words) {
            val keys = Tokenizer.tokenize(word.word).map { it.key }
            if (keys.size > 1) {
                phrases.getOrPut(keys.first()) { mutableListOf() } += Phrase(word, keys)
                continue
            }
            add(normalizeEnglish(word.word), FormMatch(word, null, word.senses.map { it.pos }.toSet()))
            val wordForms = formsOf(word)
            allForms[word.id] = wordForms
            for ((kind, text) in wordForms) add(normalizeEnglish(text), FormMatch(word, kind, posFor(kind)))
        }
        phrases.values.forEach { list -> list.sortByDescending { it.keys.size } }
    }

    fun matches(key: String): List<FormMatch> = forms[key].orEmpty()

    fun phrasesStartingWith(key: String): List<Phrase> = phrases[key].orEmpty()

    /** Headword plus every form, for the search column. */
    fun searchTerms(word: WordSource): List<String> =
        (listOf(word.word) + allForms[word.id].orEmpty().values).map { normalizeEnglish(it) }.distinct()

    private fun add(key: String, match: FormMatch) {
        forms.getOrPut(key) { mutableListOf() } += match
    }

    private fun formsOf(word: WordSource): Map<FormKind, String> {
        val pos = word.senses.map { it.pos }.toSet()
        val result = linkedMapOf<FormKind, String>()
        for (kind in FormKind.entries) {
            val explicit = word.forms.get(kind)
            // An irregular past means the past participle is listed too (go, went, gone), so no
            // regular form like goed is guessed for a missing one. Likewise good, better, best.
            val irregular = IRREGULAR_GROUPS.firstOrNull { kind in it }.orEmpty().any { word.forms.get(it) != null }
            val regular = if (!irregular && posFor(kind).any { it in pos } && kind.regularFor(pos)) {
                inflector.regular(word.word, kind)
            } else {
                null
            }
            (explicit ?: regular)?.let { result[kind] = it }
        }
        return result
    }

    private fun FormKind.regularFor(pos: Set<PartOfSpeech>): Boolean = when (this) {
        // Regular -er/-est only for adjectives; adverbs like fast list their forms.
        FormKind.COMPARATIVE, FormKind.SUPERLATIVE -> PartOfSpeech.ADJECTIVE in pos

        else -> true
    }

    companion object {
        private val IRREGULAR_GROUPS = listOf(
            setOf(FormKind.PAST, FormKind.PAST_PARTICIPLE),
            setOf(FormKind.COMPARATIVE, FormKind.SUPERLATIVE)
        )

        fun posFor(kind: FormKind): Set<PartOfSpeech> = when (kind) {
            FormKind.PLURAL, FormKind.POSSESSIVE -> setOf(PartOfSpeech.NOUN)
            FormKind.THIRD, FormKind.PAST, FormKind.PAST_PARTICIPLE, FormKind.ING -> setOf(PartOfSpeech.VERB)
            FormKind.COMPARATIVE, FormKind.SUPERLATIVE -> setOf(PartOfSpeech.ADJECTIVE, PartOfSpeech.ADVERB)
        }
    }
}

/** The explicit form of [kind], if the lexicon lists one. */
fun FormsSource.get(kind: FormKind): String? = when (kind) {
    FormKind.PLURAL -> plural
    FormKind.THIRD -> third
    FormKind.PAST -> past
    FormKind.PAST_PARTICIPLE -> pastParticiple
    FormKind.ING -> ing
    FormKind.COMPARATIVE -> comparative
    FormKind.SUPERLATIVE -> superlative
    FormKind.POSSESSIVE -> null
}
