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
package com.gyugle.gil.tools.content.compile

import com.gyugle.gil.tools.content.check.Issues
import com.gyugle.gil.tools.content.check.Knowledge
import com.gyugle.gil.tools.content.check.Limits
import com.gyugle.gil.tools.content.check.Rule
import com.gyugle.gil.tools.content.check.SentenceResolver
import com.gyugle.gil.tools.content.check.TextChecks
import com.gyugle.gil.tools.content.check.at
import com.gyugle.gil.tools.content.database.ChunkJson
import com.gyugle.gil.tools.content.database.ContentRows
import com.gyugle.gil.tools.content.database.Option
import com.gyugle.gil.tools.content.database.PayloadJson
import com.gyugle.gil.tools.content.database.SentenceRef
import com.gyugle.gil.tools.content.database.TokenJson
import com.gyugle.gil.tools.content.english.MarkedSentence
import com.gyugle.gil.tools.content.english.MarkupResult
import com.gyugle.gil.tools.content.english.SentenceMarkup
import com.gyugle.gil.tools.content.english.typographic
import com.gyugle.gil.tools.content.source.ExampleSource
import com.gyugle.gil.tools.content.source.OptionSource

/** Which language the choices of a step must be in. */
enum class OptionLanguage { ENGLISH, KOREAN, EITHER }

/**
 * Turns English sentences and answer choices into checked, stored values. Each distinct sentence
 * becomes one `sentence` row; [onGrammarLink] runs the first time a sentence links a grammar note.
 */
class SentenceCompiler(
    private val issues: Issues,
    private val text: TextChecks,
    private val resolver: SentenceResolver,
    private val rows: ContentRows,
    private val grammarIds: Set<String>,
    private val onGrammarLink: (grammarId: String, knowledge: Knowledge?) -> Unit
) {
    private val ids = mutableMapOf<List<Any?>, Long>()

    /**
     * Checks and stores one sentence. [knowledge] null skips the vocabulary check. [sentence] asks
     * for sentence punctuation; prompts and single words use false.
     */
    fun sentence(
        example: ExampleSource,
        where: String,
        knowledge: Knowledge?,
        sentence: Boolean = true,
        translated: Boolean = true
    ): SentenceRef? {
        val marked = parse(example.en, where) ?: return null
        text.english(marked.text, where, sentence = sentence)
        if (translated) text.korean(example.ko, at(where, "ko"))
        example.literal?.let { text.korean(it, at(where, "literal")) }
        example.grammar?.let { grammar ->
            if (grammar in
                grammarIds
            ) {
                onGrammarLink(grammar, knowledge)
            } else {
                issues.add(Rule.REF_MISSING, where, "no grammar note $grammar")
            }
        }
        val tokens = resolver.resolve(marked, knowledge, where)
        return store(
            marked,
            example.ko.takeIf {
                translated
            },
            tokens.map { TokenJson(it.start, it.end, it.word, it.sense) },
            example
        )
    }

    /** Parses markup and checks the vocabulary of English that is not stored as a sentence. */
    fun checkWords(english: String, where: String, knowledge: Knowledge?): String? {
        val marked = parse(english, where) ?: return null
        resolver.resolve(marked, knowledge, where)
        return marked.text
    }

    fun options(
        sources: List<OptionSource>,
        where: String,
        language: OptionLanguage,
        knowledge: Knowledge?,
        min: Int = 2
    ): List<Option> {
        if (sources.size !in min..MAX_OPTIONS) {
            issues.add(Rule.STEP_OPTIONS, where, "${sources.size} choices, give $min to $MAX_OPTIONS")
        }
        val correct = sources.count { it.correct }
        if (correct != 1) issues.add(Rule.STEP_OPTIONS, where, "$correct choices are marked correct, mark exactly one")
        val options = sources.mapIndexedNotNull { index, source ->
            option(source, at(where, "choice ${index + 1}"), language, knowledge)
        }
        options.groupBy { it.text.lowercase() }.filterValues { it.size > 1 }.keys.forEach {
            issues.add(Rule.STEP_OPTIONS, where, "the choice '$it' appears twice")
        }
        return options
    }

    private fun option(source: OptionSource, where: String, language: OptionLanguage, knowledge: Knowledge?): Option? {
        if ((source.en == null) == (source.ko == null)) {
            issues.add(Rule.STEP_OPTIONS, where, "a choice has either en or ko")
            return null
        }
        val english = source.en != null
        if ((language == OptionLanguage.ENGLISH && !english) || (language == OptionLanguage.KOREAN && english)) {
            issues.add(
                Rule.STEP_OPTIONS,
                where,
                "choices here are in ${if (language == OptionLanguage.ENGLISH) "English (en)" else "Korean (ko)"}"
            )
        }
        if (!source.correct && source.why == null) {
            issues.add(Rule.STEP_WHY, where, "say why this choice is wrong")
        }
        source.why?.let { text.korean(it, at(where, "why"), limit = Limits.PARAGRAPH, prose = true) }
        val en = source.en
        val shown = if (en != null) {
            val plain = checkWords(en, where, knowledge) ?: return null
            text.english(plain, where, limit = Limits.OPTION)
            typographic(plain)
        } else {
            val ko = source.ko.orEmpty()
            text.korean(ko, where, limit = Limits.OPTION)
            ko
        }
        return Option(shown, english, source.correct, source.why)
    }

    private fun parse(english: String, where: String): MarkedSentence? =
        when (val result = SentenceMarkup.parse(english)) {
            is MarkupResult.Parsed -> result.sentence

            is MarkupResult.Invalid -> {
                issues.add(Rule.MARKUP_INVALID, where, "${result.reason}: $english")
                null
            }
        }

    private fun store(
        marked: MarkedSentence,
        ko: String?,
        tokens: List<TokenJson>,
        example: ExampleSource
    ): SentenceRef {
        val shown = typographic(marked.text)
        val tokensJson = PayloadJson.encodeToString(tokens)
        val chunksJson = marked.chunks.takeIf { it.isNotEmpty() }
            ?.let { chunks -> PayloadJson.encodeToString(chunks.map { ChunkJson(it.start, it.end, it.role.key) }) }
        val key = listOf(shown, ko, tokensJson, chunksJson, example.literal, example.grammar)
        val id = ids.getOrPut(key) {
            val next = ids.size + 1L
            rows.add(
                "sentence",
                "id" to next,
                "text" to shown,
                "translationKo" to ko,
                "tokens" to tokensJson,
                "chunks" to chunksJson,
                "literalKo" to example.literal,
                "grammarId" to example.grammar
            )
            next
        }
        return SentenceRef(id, shown, ko)
    }

    private companion object {
        const val MAX_OPTIONS = 4
    }
}
