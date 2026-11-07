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
import com.gyugle.gil.tools.content.check.LexiconIndex
import com.gyugle.gil.tools.content.check.Limits
import com.gyugle.gil.tools.content.check.Rule
import com.gyugle.gil.tools.content.check.TextChecks
import com.gyugle.gil.tools.content.check.at
import com.gyugle.gil.tools.content.database.Line
import com.gyugle.gil.tools.content.database.PairWord
import com.gyugle.gil.tools.content.database.Question
import com.gyugle.gil.tools.content.database.StepPayload
import com.gyugle.gil.tools.content.english.normalizeEnglish
import com.gyugle.gil.tools.content.english.typographic
import com.gyugle.gil.tools.content.source.ArrangeSource
import com.gyugle.gil.tools.content.source.ChooseEnglishSource
import com.gyugle.gil.tools.content.source.ChooseMeaningSource
import com.gyugle.gil.tools.content.source.DialogueSource
import com.gyugle.gil.tools.content.source.DictationSource
import com.gyugle.gil.tools.content.source.ExampleSource
import com.gyugle.gil.tools.content.source.ExplainSource
import com.gyugle.gil.tools.content.source.FillBlankSource
import com.gyugle.gil.tools.content.source.ListenChooseSource
import com.gyugle.gil.tools.content.source.ListenSource
import com.gyugle.gil.tools.content.source.MinimalPairSource
import com.gyugle.gil.tools.content.source.ReadPassageSource
import com.gyugle.gil.tools.content.source.ShadowSource
import com.gyugle.gil.tools.content.source.SpellSource
import com.gyugle.gil.tools.content.source.StepSource

/** Checks one lesson step against what the learner knows at that lesson and builds its payload. */
class StepCompiler(
    private val issues: Issues,
    private val text: TextChecks,
    private val sentences: SentenceCompiler,
    private val lexicon: LexiconIndex,
    private val studyItems: Set<String>,
    /** IPA of a drill word; reports a missing or ambiguous pronunciation at the given place. */
    private val ipaOf: (word: String, where: String) -> String?,
    private val wordIpa: Map<String, String>
) {

    /** [knowledge] is everything taught up to and including this lesson. */
    fun compile(step: StepSource, where: String, knowledge: Knowledge): StepPayload? {
        val words = knowledge.takeIf { step.checkVocab }
        checkItem(step.item, where, knowledge)
        return when (step) {
            is ExplainSource -> explain(step, where, words)

            is ListenSource -> sentences.sentence(
                ExampleSource(step.en, step.ko, step.literal, step.grammar),
                where,
                words
            )
                ?.let { StepPayload.Listen(it) }

            is ChooseMeaningSource -> {
                val prompt = sentences.sentence(
                    ExampleSource(step.en, ""),
                    where,
                    words,
                    sentence = false,
                    translated = false
                )
                val options = sentences.options(step.options, where, OptionLanguage.KOREAN, words, min = 3)
                prompt?.let { StepPayload.ChooseMeaning(it, options, step.item) }
            }

            is ListenChooseSource -> {
                val audio = sentences.sentence(
                    ExampleSource(step.en, ""),
                    where,
                    words,
                    sentence = false,
                    translated = false
                )
                val options = sentences.options(step.options, where, OptionLanguage.EITHER, words)
                audio?.let { StepPayload.ListenChoose(it, options, step.item) }
            }

            is ChooseEnglishSource -> {
                text.korean(step.ko, at(where, "ko"))
                StepPayload.ChooseEnglish(
                    step.ko,
                    sentences.options(step.options, where, OptionLanguage.ENGLISH, words),
                    step.item
                )
            }

            is ArrangeSource -> arrange(step, where, words)

            is FillBlankSource -> fillBlank(step, where, words)

            is SpellSource -> spell(step, where)

            is DictationSource -> sentences.sentence(ExampleSource(step.en, step.ko), where, words)
                ?.let { StepPayload.Dictation(it, step.item) }

            is MinimalPairSource -> minimalPair(step, where)

            is ShadowSource -> sentences.sentence(ExampleSource(step.en, step.ko), where, words)
                ?.let { StepPayload.Shadow(it) }

            is ReadPassageSource -> readPassage(step, where, words)

            is DialogueSource -> dialogue(step, where, words)
        }
    }

    private fun checkItem(item: String?, where: String, knowledge: Knowledge) {
        if (item == null) return
        when {
            STUDY_ITEM_PREFIXES.none { item.startsWith(it) } ->
                issues.add(Rule.STEP_ITEM, where, "$item is not a sense, pattern or sound")

            item !in studyItems -> issues.add(Rule.REF_MISSING, where, "no study item $item")

            item !in knowledge.items -> issues.add(
                Rule.VOCAB_NOT_YET,
                where,
                "the step tests $item, which is not taught by this lesson or an earlier one"
            )
        }
    }

    private fun explain(step: ExplainSource, where: String, words: Knowledge?): StepPayload {
        val paragraphs = step.text.split(BLANK_LINE).map { it.trim().replace('\n', ' ') }.filter { it.isNotEmpty() }
        if (paragraphs.isEmpty()) issues.add(Rule.TEXT_EMPTY, where, "the explanation has no text")
        paragraphs.forEachIndexed { index, paragraph ->
            text.korean(paragraph, at(where, "paragraph ${index + 1}"), limit = Limits.PARAGRAPH, prose = true)
        }
        val examples = step.examples.mapIndexedNotNull { index, example ->
            sentences.sentence(example, at(where, "example ${index + 1}"), words)
        }
        return StepPayload.Explain(paragraphs, examples)
    }

    private fun arrange(step: ArrangeSource, where: String, words: Knowledge?): StepPayload? {
        text.korean(step.ko, at(where, "ko"))
        val answer = sentences.sentence(ExampleSource(step.en, step.ko), where, words) ?: return null
        val tiles = tiles(answer.text)
        tiles.filter { it.length > Limits.TILE }.forEach {
            issues.add(Rule.TEXT_LENGTH, where, "the tile '$it' is longer than ${Limits.TILE} characters")
        }
        val accepted = step.accept.mapIndexedNotNull { index, other ->
            val plain =
                sentences.checkWords(other, at(where, "accept ${index + 1}"), words) ?: return@mapIndexedNotNull null
            val order = tiles(typographic(plain))
            if (order.map(::normalizeEnglish).sorted() != tiles.map(::normalizeEnglish).sorted()) {
                issues.add(
                    Rule.STEP_TILES,
                    at(where, "accept ${index + 1}"),
                    "'$other' does not use the same tiles as the answer"
                )
            }
            // Stored with the answer's spelling (Yesterday, not yesterday) so the app compares tiles as shown.
            order.map { tile -> tiles.firstOrNull { normalizeEnglish(it) == normalizeEnglish(tile) } ?: tile }
        }
        if (step.extra.size > MAX_EXTRA_TILES) {
            issues.add(Rule.STEP_TILES, where, "${step.extra.size} extra tiles, use at most $MAX_EXTRA_TILES")
        }
        val extra = step.extra.mapIndexedNotNull { index, tile ->
            val place = at(where, "extra ${index + 1}")
            sentences.checkWords(tile, place, words)?.also {
                text.english(it, place, limit = Limits.TILE, spoken = false)
            }
                ?.let(::typographic)
        }
        return StepPayload.Arrange(step.ko, answer, tiles, accepted, extra, step.item)
    }

    private fun fillBlank(step: FillBlankSource, where: String, words: Knowledge?): StepPayload? {
        val blanks = BLANK.findAll(step.en).count()
        if (blanks != 1) {
            issues.add(Rule.STEP_BLANK, where, "$blanks blanks, write exactly one as ___")
            return null
        }
        text.korean(step.ko, at(where, "ko"))
        if (step.answers.isEmpty() == step.options.isEmpty()) {
            issues.add(Rule.STEP_BLANK, where, "give either typed answers or choices")
            return null
        }
        val options = if (step.options.isEmpty()) {
            emptyList()
        } else {
            sentences.options(
                step.options,
                where,
                OptionLanguage.ENGLISH,
                words
            )
        }
        val answers = step.answers.mapIndexed { index, answer ->
            text.english(answer, at(where, "answer ${index + 1}"), limit = Limits.OPTION, spoken = false)
            typographic(answer)
        }
        val fills = step.answers.ifEmpty { step.options.filter { it.correct }.mapNotNull { it.en } }
        val first = fills.firstOrNull() ?: return null
        fills.drop(1).forEachIndexed { index, fill ->
            sentences.checkWords(step.en.replace(BLANK) { fill }, at(where, "answer ${index + 2}"), words)
        }
        val filled =
            sentences.sentence(ExampleSource(step.en.replace(BLANK) { first }, step.ko), where, words) ?: return null
        val shown = sentences.checkWords(step.en, where, knowledge = null) ?: return null
        return StepPayload.FillBlank(typographic(shown), step.ko, answers, options, filled, step.item)
    }

    private fun spell(step: SpellSource, where: String): StepPayload? {
        val sense = lexicon.senses[step.sense] ?: return null
        val word = lexicon.senseWord.getValue(step.sense)
        val ipa = sense.ipa?.trim('/') ?: wordIpa[word.id] ?: return null
        return StepPayload.Spell(step.sense, word.id, typographic(word.word), sense.ko, ipa)
    }

    private fun minimalPair(step: MinimalPairSource, where: String): StepPayload? {
        if (step.words.size != 2) {
            issues.add(Rule.STEP_OPTIONS, where, "a minimal pair has two words")
            return null
        }
        val answer = step.words.indexOf(step.answer)
        if (answer < 0) {
            issues.add(Rule.STEP_OPTIONS, where, "the answer '${step.answer}' is not one of the two words")
            return null
        }
        step.why?.let { text.korean(it, at(where, "why"), limit = Limits.PARAGRAPH, prose = true) }
        val pair = step.words.map { word ->
            text.english(word, where, limit = Limits.OPTION)
            PairWord(typographic(word), ipaOf(word, where).orEmpty())
        }
        return StepPayload.MinimalPair(pair, answer, step.why, step.item)
    }

    private fun readPassage(step: ReadPassageSource, where: String, words: Knowledge?): StepPayload {
        if (step.sentences.isEmpty()) issues.add(Rule.TEXT_EMPTY, where, "the passage has no sentences")
        val passage = step.sentences.mapIndexedNotNull { index, sentence ->
            sentences.sentence(sentence, at(where, "sentence ${index + 1}"), words)
        }
        if (step.questions.isEmpty()) issues.add(Rule.STEP_OPTIONS, where, "the passage has no questions")
        val questions = step.questions.mapIndexed { index, question ->
            val place = at(where, "question ${index + 1}")
            text.korean(question.ko, place)
            Question(question.ko, sentences.options(question.options, place, OptionLanguage.EITHER, words))
        }
        return StepPayload.ReadPassage(passage, questions)
    }

    private fun dialogue(step: DialogueSource, where: String, words: Knowledge?): StepPayload {
        if (step.lines.isEmpty()) issues.add(Rule.TEXT_EMPTY, where, "the dialogue has no lines")
        val lines = step.lines.mapIndexedNotNull { index, line ->
            val place = at(where, "line ${index + 1}")
            text.english(line.speaker, at(place, "speaker"), spoken = false)
            sentences.sentence(ExampleSource(line.en, line.ko), place, words)?.let { Line(line.speaker, it) }
        }
        step.question?.let { text.korean(it, at(where, "question")) }
        val options = sentences.options(step.options, where, OptionLanguage.ENGLISH, words)
        return StepPayload.Dialogue(lines, step.question, options, step.item)
    }

    /** Words of a sentence as tiles, without the punctuation around them. */
    private fun tiles(sentence: String): List<String> =
        sentence.split(WHITESPACE).map { it.trim { c -> !c.isLetterOrDigit() } }.filter { it.isNotEmpty() }

    private companion object {
        val BLANK = Regex("_{3,}")
        val BLANK_LINE = Regex("\\n\\s*\\n")
        val WHITESPACE = Regex("\\s+")
        const val MAX_EXTRA_TILES = 2
        val STUDY_ITEM_PREFIXES = listOf("sense.", "pattern.", "sound.")
    }
}
