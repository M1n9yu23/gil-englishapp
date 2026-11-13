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
import com.gyugle.gil.tools.content.check.SentenceResolver
import com.gyugle.gil.tools.content.check.TextChecks
import com.gyugle.gil.tools.content.check.at
import com.gyugle.gil.tools.content.check.get
import com.gyugle.gil.tools.content.database.ContentRows
import com.gyugle.gil.tools.content.database.PayloadJson
import com.gyugle.gil.tools.content.database.SentenceRef
import com.gyugle.gil.tools.content.database.StepPayload
import com.gyugle.gil.tools.content.english.FormKind
import com.gyugle.gil.tools.content.english.Inflector
import com.gyugle.gil.tools.content.english.Tokenizer
import com.gyugle.gil.tools.content.english.apostrophes
import com.gyugle.gil.tools.content.english.normalizeEnglish
import com.gyugle.gil.tools.content.english.typographic
import com.gyugle.gil.tools.content.font.FontCoverage
import com.gyugle.gil.tools.content.pronunciation.Cmudict
import com.gyugle.gil.tools.content.pronunciation.Ipa
import com.gyugle.gil.tools.content.source.ExampleSource
import com.gyugle.gil.tools.content.source.LessonSource
import com.gyugle.gil.tools.content.source.LoadedContent
import com.gyugle.gil.tools.content.source.Localized
import com.gyugle.gil.tools.content.source.StageSource
import com.gyugle.gil.tools.content.source.UnitSource

/** One lesson in course order, for the report. */
data class LessonSummary(
    val id: String,
    val unitId: String,
    val stageId: String,
    val title: String,
    val introduces: List<String>,
    val steps: Int,
    val uncheckedSteps: Int
)

class CompiledContent(val rows: ContentRows, val lessons: List<LessonSummary>, val ids: Set<String>)

/**
 * Checks the whole course and turns it into database rows. Lessons are walked in course order so
 * every sentence is checked against what the learner has met by then.
 */
class ContentAnalyzer(
    private val content: LoadedContent,
    private val cmudict: Cmudict,
    private val issues: Issues,
    uiFont: FontCoverage?,
    englishFont: FontCoverage?
) {
    private val text = TextChecks(issues, uiFont, englishFont)
    private val rows = ContentRows()
    private val ids = linkedMapOf<String, String>()
    private val words = content.words.map { it.value }
    private val lexicon = LexiconIndex(dedupe(words) { it.id }, Inflector(cmudict))
    private val patterns = dedupe(content.patterns.map { it.value }) { it.id }.associateBy { it.id }
    private val sounds = dedupe(content.sounds.map { it.value }) { it.id }.associateBy { it.id }
    private val grammar = dedupe(content.grammar.map { it.value }) { it.id }.associateBy { it.id }
    private val studyItems = lexicon.senses.keys + patterns.keys + sounds.keys

    /** Item ID to the lesson and the stage that introduce it. */
    private val introducedIn = linkedMapOf<String, String>()
    private val introducedStage = mutableMapOf<String, String>()
    private val wordIpa = mutableMapOf<String, String>()
    private val examples = mutableMapOf<String, List<SentenceRef>>()
    private val placedUnits = mutableListOf<Pair<UnitSource, StageSource>>()

    private class Placed(val lesson: LessonSource, val unit: UnitSource, val stage: StageSource, val file: String)

    fun analyze(): CompiledContent {
        val course = content.course?.value
        val order = if (course != null) courseOrder(course.stages) else emptyList()
        checkLexicon()
        checkOtherItems()
        checkLock()
        introduce(order)
        val gates = gates(course?.inflections.orEmpty())
        val lessonOrder = order.mapIndexed { index, placed -> placed.lesson.id to index }.toMap()
        val resolver = SentenceResolver(lexicon, gates, introducedIn, lessonOrder, issues)
        lateinit var sentences: SentenceCompiler
        val linkedGrammar = mutableSetOf<String>()
        sentences = SentenceCompiler(issues, text, resolver, rows, grammar.keys) { id, knowledge ->
            // A link from a step without the vocabulary check does not count as the first link.
            if (knowledge != null && linkedGrammar.add(id)) {
                examples[id] =
                    exampleRefs(grammar.getValue(id).examples, "grammar $id", knowledge, sentences)
            }
        }
        val steps = StepCompiler(issues, text, sentences, lexicon, studyItems, { word, at -> ipaOf(word, at) }, wordIpa)
        val summaries = walk(order, sentences, steps)
        // Items no lesson introduces yet and grammar no sentence links: stored, but not checked against vocabulary.
        lexicon.senses.values.filter { it.id !in examples }.forEach {
            examples[it.id] = exampleRefs(it.examples, it.id, null, sentences)
        }
        patterns.values.filter { it.id !in examples }.forEach {
            examples[it.id] = exampleRefs(it.examples, it.id, null, sentences)
        }
        grammar.values.filter { it.id !in linkedGrammar }.forEach {
            examples[it.id] = exampleRefs(it.examples, "grammar ${it.id}", null, sentences)
        }
        if (course != null) writeRows(course.stages, order)
        return CompiledContent(rows, summaries, ids.keys)
    }

    private fun courseOrder(stages: List<StageSource>): List<Placed> {
        content.units.groupBy { it.value.id }.filterValues { it.size > 1 }.forEach { (id, files) ->
            issues.add(Rule.ID_DUPLICATE, files.last().file, "$id is also defined in ${files.first().file}")
        }
        val units = content.units.associateBy { it.value.id }
        val listed = mutableSetOf<String>()
        val order = mutableListOf<Placed>()
        for (stage in stages) {
            val stageWhere = at("course.yaml", stage.id)
            claim(stage.id, STAGE, stageWhere)
            localized(stage.title, at(stageWhere, "title"))
            stage.canDo.forEachIndexed { i, canDo -> localized(canDo, at(stageWhere, "canDo ${i + 1}")) }
            for (unitId in stage.units) {
                if (!listed.add(unitId)) {
                    issues.add(Rule.ID_DUPLICATE, stageWhere, "$unitId is listed more than once")
                    continue
                }
                val located = units[unitId]
                if (located == null) {
                    issues.add(Rule.REF_MISSING, stageWhere, "no file for $unitId")
                    continue
                }
                val unit = located.value
                val code = UNIT.matchEntire(unitId)?.groupValues?.get(1)
                claim(unitId, UNIT, located.file)
                if (code != null) {
                    if (stage.id != "stage.${code.substringBefore('-')}") {
                        issues.add(Rule.ID_PATH, located.file, "$unitId is listed under ${stage.id}")
                    }
                    if (located.file != "units/${code.substringBefore('-')}/$code.yaml") {
                        issues.add(
                            Rule.ID_PATH,
                            located.file,
                            "$unitId belongs in units/${code.substringBefore('-')}/$code.yaml"
                        )
                    }
                }
                localized(unit.title, at(located.file, "title"))
                if (unit.lessons.isEmpty()) issues.add(Rule.TEXT_EMPTY, located.file, "$unitId has no lessons")
                placedUnits += unit to stage
                for (lesson in unit.lessons) {
                    val lessonWhere = at(located.file, lesson.id)
                    if (claim(lesson.id, LESSON, lessonWhere) && code != null &&
                        !lesson.id.startsWith("lesson.$code.")
                    ) {
                        issues.add(Rule.ID_FORMAT, lessonWhere, "lessons of $unitId are named lesson.$code.<n>")
                    }
                    localized(lesson.title, at(lessonWhere, "title"))
                    localized(lesson.goal, at(lessonWhere, "goal"))
                    order += Placed(lesson, unit, stage, located.file)
                }
            }
        }
        content.units.filter { it.value.id !in listed }.forEach {
            issues.add(Rule.REF_UNLISTED, it.file, "${it.value.id} is not listed in course.yaml")
        }
        return order
    }

    private fun checkLexicon() {
        val headwords = mutableMapOf<String, String>()
        for (located in content.words) {
            val word = located.value
            val where = at(located.file, word.id)
            val slug = slug(word.word)
            if (claim(word.id, WORD, where) && word.id != "word.$slug") {
                issues.add(Rule.ID_FORMAT, where, "the ID of '${word.word}' is word.$slug")
            }
            headwords.put(normalizeEnglish(word.word), word.id)?.let {
                issues.add(Rule.ID_DUPLICATE, where, "'${word.word}' is already an entry ($it)")
            }
            text.english(word.word, at(where, "word"), spoken = false)
            word.hangul?.let { text.korean(it, at(where, "hangul")) }
            FormKind.entries.mapNotNull { kind -> word.forms.get(kind)?.let { kind to it } }.forEach { (kind, form) ->
                text.english(form, at(where, "forms", kind.key), spoken = false)
            }
            word.family.forEach { text.english(it, at(where, "family"), spoken = false) }
            word.collocations.forEachIndexed { i, c ->
                text.english(c.en, at(where, "collocation ${i + 1}"))
                text.korean(c.ko, at(where, "collocation ${i + 1}", "ko"))
            }
            word.notes.forEachIndexed { i, note ->
                text.korean(note.ko, at(where, "note ${i + 1}"), Limits.PARAGRAPH, prose = true)
            }
            wordPronunciation(word.word, word.ipa, where)?.let { wordIpa[word.id] = it }
            if (word.senses.isEmpty()) issues.add(Rule.EXAMPLE_MISSING, where, "the word has no senses")
            for (sense in word.senses) {
                val senseWhere = at(located.file, sense.id)
                val match = SENSE.matchEntire(sense.id)
                if (claim(sense.id, SENSE, senseWhere) && match != null) {
                    val (wordSlug, pos) = match.destructured
                    if (wordSlug != slug || pos != sense.pos.abbreviation) {
                        issues.add(
                            Rule.ID_FORMAT,
                            senseWhere,
                            "senses of '${word.word}' as ${sense.pos.key} are named sense.$slug.${sense.pos.abbreviation}.<meaning>"
                        )
                    }
                }
                text.korean(sense.ko, at(senseWhere, "ko"))
                sense.definition?.let { text.english(it, at(senseWhere, "definition")) }
                sense.ipa?.let { text.ipa(it.trim('/'), at(senseWhere, "ipa")) }
                checkLevel(sense.level, senseWhere)
                if (sense.examples.isEmpty()) {
                    issues.add(
                        Rule.EXAMPLE_MISSING,
                        senseWhere,
                        "every sense needs at least one example"
                    )
                }
            }
        }
    }

    private fun checkOtherItems() {
        for (located in content.patterns) {
            val pattern = located.value
            val where = at(located.file, pattern.id)
            claim(pattern.id, PATTERN, where)
            text.english(pattern.text, at(where, "text"), spoken = false)
            text.korean(pattern.ko, at(where, "ko"))
            pattern.note?.let { text.korean(it, at(where, "note"), Limits.PARAGRAPH, prose = true) }
            checkLevel(pattern.level, where)
            if (pattern.examples.isEmpty()) {
                issues.add(
                    Rule.EXAMPLE_MISSING,
                    where,
                    "every pattern needs at least one example"
                )
            }
        }
        for (located in content.sounds) {
            val sound = located.value
            val where = at(located.file, sound.id)
            claim(sound.id, SOUND, where)
            text.ipa(sound.symbol, at(where, "symbol"))
            text.english(sound.spelling, at(where, "spelling"), spoken = false)
            text.korean(sound.ko, at(where, "ko"), Limits.PARAGRAPH, prose = true)
            checkLevel(sound.level, where)
            if (sound.examples.isEmpty()) {
                issues.add(
                    Rule.EXAMPLE_MISSING,
                    where,
                    "every sound needs at least one example word"
                )
            }
            sound.examples.forEach { example ->
                text.english(example, at(where, "example"), spoken = false)
                ipaOf(example, where)
            }
        }
        for (located in content.grammar) {
            val note = located.value
            val where = at(located.file, note.id)
            claim(note.id, GRAMMAR, where)
            text.korean(note.title, at(where, "title"))
            paragraphs(note.body).forEachIndexed { i, paragraph ->
                text.korean(paragraph, at(where, "paragraph ${i + 1}"), Limits.PARAGRAPH, prose = true)
            }
        }
    }

    private fun checkLock() {
        val lock = content.lock
        (lock.locked - lock.retired).filter { it !in ids }.forEach {
            issues.add(Rule.LOCK_MISSING, "ids.lock", "$it was published and is gone; restore it or mark it retired")
        }
        lock.retired.filter { it in ids }.forEach {
            issues.add(Rule.LOCK_REUSED, ids.getValue(it), "$it was retired and may not be used again")
        }
    }

    private fun introduce(order: List<Placed>) {
        for (placed in order) {
            val where = at(placed.file, placed.lesson.id)
            val stageCode = placed.stage.id.substringAfter("stage.")
            for (item in placed.lesson.introduces) {
                val level = when {
                    item.startsWith("sense.") -> lexicon.senses[item]?.let { it.level to true }
                    item.startsWith("pattern.") -> patterns[item]?.let { it.level to true }
                    item.startsWith("sound.") -> sounds[item]?.let { it.level to true }
                    else -> null
                }
                when {
                    level == null -> issues.add(
                        Rule.REF_MISSING,
                        where,
                        "introduces $item, which is not a sense, pattern or sound in the content"
                    )

                    item in introducedIn -> issues.add(
                        Rule.INTRODUCE_TWICE,
                        where,
                        "$item is already introduced in ${introducedIn[item]}"
                    )

                    else -> {
                        introducedIn[item] = placed.lesson.id
                        introducedStage[item] = placed.stage.id
                        if (level.first != null && level.first != stageCode) {
                            issues.add(
                                Rule.INTRODUCE_LEVEL,
                                where,
                                "$item has level ${level.first} but is introduced in stage $stageCode; remove the level"
                            )
                        }
                    }
                }
            }
        }
        val unplaced = lexicon.senses.values.filter { it.id !in introducedIn && it.level == null }.map { it.id } +
            patterns.values.filter { it.id !in introducedIn && it.level == null }.map { it.id } +
            sounds.values.filter { it.id !in introducedIn && it.level == null }.map { it.id }
        unplaced.forEach {
            issues.add(Rule.LEVEL_MISSING, ids[it] ?: it, "$it is not introduced by any lesson, so give it a level")
        }
    }

    private fun gates(inflections: Map<String, String>): Map<FormKind, String> = inflections.mapNotNull { (key, item) ->
        val kind = FormKind.fromKey(key)
        when {
            kind == null -> {
                issues.add(
                    Rule.REF_MISSING,
                    "course.yaml › inflections",
                    "unknown inflection '$key', use one of ${FormKind.entries.joinToString {
                        it.key
                    }}"
                )
                null
            }

            item !in studyItems -> {
                issues.add(
                    Rule.REF_MISSING,
                    "course.yaml › inflections",
                    "$key is gated by $item, which is not in the content"
                )
                null
            }

            else -> kind to item
        }
    }.toMap()

    private fun walk(order: List<Placed>, sentences: SentenceCompiler, steps: StepCompiler): List<LessonSummary> {
        val knowledge = Knowledge()
        return order.mapIndexed { index, placed ->
            val lesson = placed.lesson
            val where = at(placed.file, lesson.id)
            val introduced = lesson.introduces.filter { introducedIn[it] == lesson.id }
            knowledge.items += introduced
            introduced.mapNotNull { patterns[it] }.forEach { pattern ->
                knowledge.literals += Tokenizer.tokenize(pattern.text.replace('~', ' ')).map { it.key }
            }
            if (lesson.steps.isEmpty()) issues.add(Rule.TEXT_EMPTY, where, "the lesson has no steps")
            lesson.steps.forEachIndexed { i, step ->
                val payload = steps.compile(step, at(where, "step ${i + 1}"), knowledge)
                if (payload != null) {
                    rows.add(
                        "lesson_step",
                        "lessonId" to lesson.id,
                        "position" to i,
                        "payload" to PayloadJson.encodeToString(StepPayload.serializer(), payload)
                    )
                }
            }
            for (item in introduced) {
                val source = lexicon.senses[item]?.examples ?: patterns[item]?.examples ?: continue
                examples[item] = exampleRefs(source, at(where, item), knowledge, sentences)
            }
            rows.add(
                "lesson",
                "id" to lesson.id,
                "unitId" to placed.unit.id,
                "position" to placed.unit.lessons.indexOf(lesson),
                "coursePosition" to index,
                "titleKo" to apostrophes(lesson.title.ko),
                "titleEn" to apostrophes(lesson.title.en),
                "goalKo" to apostrophes(lesson.goal.ko),
                "goalEn" to apostrophes(lesson.goal.en)
            )
            introduced.forEachIndexed { i, item ->
                rows.add(
                    "lesson_item",
                    "lessonId" to lesson.id,
                    "position" to i,
                    "itemId" to item
                )
            }
            LessonSummary(
                lesson.id,
                placed.unit.id,
                placed.stage.id,
                lesson.title.ko,
                introduced,
                lesson.steps.size,
                lesson.steps.count { !it.checkVocab }
            )
        }
    }

    private fun exampleRefs(
        sources: List<ExampleSource>,
        where: String,
        knowledge: Knowledge?,
        sentences: SentenceCompiler
    ): List<SentenceRef> {
        val snapshot = knowledge?.let { k ->
            Knowledge().also {
                it.items += k.items
                it.literals += k.literals
            }
        }
        return sources.mapIndexedNotNull { i, example ->
            sentences.sentence(example, at(where, "example ${i + 1}"), snapshot)
        }
    }

    private fun writeRows(stages: List<StageSource>, order: List<Placed>) {
        stages.forEachIndexed { i, stage ->
            rows.add(
                "stage",
                "id" to stage.id,
                "position" to i,
                "titleKo" to apostrophes(stage.title.ko),
                "titleEn" to apostrophes(stage.title.en)
            )
            stage.canDo.forEachIndexed { k, canDo ->
                rows.add(
                    "stage_can_do",
                    "stageId" to stage.id,
                    "position" to k,
                    "textKo" to apostrophes(canDo.ko),
                    "textEn" to apostrophes(canDo.en)
                )
            }
        }
        placedUnits.forEach { (unit, stage) ->
            rows.add(
                "unit",
                "id" to unit.id,
                "stageId" to stage.id,
                "position" to stage.units.indexOf(unit.id),
                "titleKo" to apostrophes(unit.title.ko),
                "titleEn" to apostrophes(unit.title.en)
            )
        }
        for (word in lexicon.words.values) {
            rows.add(
                "word",
                "id" to word.id,
                "text" to typographic(word.word),
                "ipa" to wordIpa[word.id].orEmpty(),
                "hangul" to word.hangul,
                "searchKey" to " " + lexicon.searchTerms(word).joinToString(" ")
            )
            FormKind.entries.forEach { kind ->
                word.forms.get(kind)?.let {
                    rows.add(
                        "word_form",
                        "wordId" to word.id,
                        "kind" to kind.key,
                        "text" to typographic(it)
                    )
                }
            }
            word.notes.forEachIndexed { i, note ->
                rows.add(
                    "word_note",
                    "wordId" to word.id,
                    "position" to i,
                    "kind" to note.kind.name.lowercase(),
                    "textKo" to note.ko
                )
            }
            word.collocations.forEachIndexed { i, c ->
                rows.add(
                    "word_collocation",
                    "wordId" to word.id,
                    "position" to i,
                    "textEn" to typographic(c.en),
                    "textKo" to c.ko
                )
            }
            word.family.forEachIndexed { i, member ->
                val related = lexicon.words["word.${slug(member)}"]?.id
                rows.add(
                    "word_family",
                    "wordId" to word.id,
                    "position" to i,
                    "text" to typographic(member),
                    "relatedWordId" to related
                )
            }
            word.senses.forEachIndexed { i, sense ->
                rows.add(
                    "sense",
                    "id" to sense.id,
                    "wordId" to word.id,
                    "position" to i,
                    "pos" to sense.pos.key,
                    "meaningKo" to sense.ko,
                    "definitionEn" to sense.definition,
                    "ipa" to sense.ipa?.trim('/'),
                    "stageId" to stageOf(sense.id, sense.level),
                    "lessonId" to introducedIn[sense.id]
                )
                exampleRows("sense_example", "senseId", sense.id)
            }
        }
        for (pattern in patterns.values) {
            rows.add(
                "pattern",
                "id" to pattern.id,
                "text" to typographic(pattern.text),
                "meaningKo" to pattern.ko,
                "noteKo" to pattern.note,
                "stageId" to stageOf(pattern.id, pattern.level),
                "lessonId" to introducedIn[pattern.id]
            )
            exampleRows("pattern_example", "patternId", pattern.id)
        }
        for (sound in sounds.values) {
            rows.add(
                "sound",
                "id" to sound.id,
                "symbol" to sound.symbol.trim('/'),
                "spelling" to sound.spelling,
                "descriptionKo" to sound.ko,
                "stageId" to stageOf(sound.id, sound.level),
                "lessonId" to introducedIn[sound.id]
            )
            sound.examples.forEachIndexed { i, example ->
                rows.add(
                    "sound_example",
                    "soundId" to sound.id,
                    "position" to i,
                    "text" to typographic(example),
                    "ipa" to ipaOf(example).orEmpty()
                )
            }
        }
        for (note in grammar.values) {
            rows.add(
                "grammar",
                "id" to note.id,
                "titleKo" to note.title,
                "bodyKo" to paragraphs(note.body).joinToString("\n\n")
            )
            exampleRows("grammar_example", "grammarId", note.id)
        }
    }

    private fun exampleRows(table: String, ownerColumn: String, owner: String) {
        examples[owner].orEmpty().forEachIndexed { i, ref ->
            rows.add(table, ownerColumn to owner, "position" to i, "sentenceId" to ref.id)
        }
    }

    private fun stageOf(id: String, level: String?): String =
        introducedStage[id] ?: "stage.${level ?: STAGE_CODES.first()}"

    /**
     * IPA of the lexicon word spelled [word], or of CMUdict's first pronunciation. With [where], a
     * missing or ambiguous CMUdict entry is reported, since a drill word must sound as intended.
     */
    private fun ipaOf(word: String, where: String? = null): String? {
        lexicon.words["word.${slug(word)}"]?.let { entry -> wordIpa[entry.id]?.let { return it } }
        val found = cmudictIpa(word)
        if (where != null) {
            if (found == null) {
                issues.add(Rule.IPA_MISSING, where, "no IPA for '$word': add it to the lexicon with ipa")
            } else if (found.second) {
                issues.add(
                    Rule.IPA_VARIANTS,
                    where,
                    "CMUdict has several pronunciations of '$word'; add it to the lexicon with ipa"
                )
            }
        }
        return found?.first
    }

    /** Returns the IPA and whether CMUdict had more than one pronunciation. */
    private fun cmudictIpa(word: String): Pair<String, Boolean>? {
        val parts = Tokenizer.tokenize(word).map { cmudict.pronunciations(it.key) }
        if (parts.isEmpty() || parts.any { it.isEmpty() }) return null
        return parts.joinToString(" ") { Ipa.fromArpabet(it.first()) } to parts.any { it.size > 1 }
    }

    private fun wordPronunciation(word: String, explicit: String?, where: String): String? {
        if (explicit != null) {
            val ipa = explicit.trim('/')
            text.ipa(ipa, at(where, "ipa"))
            return ipa
        }
        val found = cmudictIpa(word)
        if (found == null) {
            issues.add(Rule.IPA_MISSING, where, "'$word' is not in CMUdict; add ipa")
            return null
        }
        if (found.second) {
            val options = Tokenizer.tokenize(word).flatMap { cmudict.pronunciations(it.key) }
                .joinToString(", ") { "/${Ipa.fromArpabet(it)}/" }
            issues.add(
                Rule.IPA_VARIANTS,
                where,
                "CMUdict has several pronunciations ($options); /${found.first}/ is used, set ipa to choose"
            )
        }
        return found.first
    }

    private fun localized(value: Localized, where: String) {
        text.korean(value.ko, at(where, "ko"))
        text.chrome(value.en, at(where, "en"))
    }

    private fun checkLevel(level: String?, where: String) {
        if (level != null && level !in STAGE_CODES) {
            issues.add(Rule.LEVEL_MISSING, where, "level '$level' is not one of ${STAGE_CODES.joinToString()}")
        }
    }

    /** Records [id] and checks its format. Returns false when it is malformed or taken. */
    private fun claim(id: String, format: Regex, where: String): Boolean {
        if (!format.matches(id)) {
            issues.add(Rule.ID_FORMAT, where, "'$id' does not match ${format.pattern}")
            return false
        }
        val previous = ids.putIfAbsent(id, where)
        if (previous != null) {
            issues.add(Rule.ID_DUPLICATE, where, "$id is already used at $previous")
            return false
        }
        return true
    }

    private fun <T> dedupe(items: List<T>, key: (T) -> String): List<T> = items.distinctBy(key)

    private fun paragraphs(body: String): List<String> =
        body.split(Regex("\\n\\s*\\n")).map { it.trim().replace('\n', ' ') }.filter { it.isNotEmpty() }

    companion object {
        val STAGE_CODES = listOf("s0", "s1", "a1", "a2", "b1", "b2", "c1")
        private const val SLUG = "[a-z0-9]+(?:-[a-z0-9]+)*"
        private val CODE = STAGE_CODES.joinToString("|")
        val STAGE = Regex("stage\\.(?:$CODE)")
        val UNIT = Regex("unit\\.((?:$CODE)-\\d{2})")
        val LESSON = Regex("lesson\\.(?:$CODE)-\\d{2}\\.[1-9]\\d*")
        val WORD = Regex("word\\.$SLUG")
        val SENSE = Regex("sense\\.($SLUG)\\.([a-z]+)\\.$SLUG")
        val PATTERN = Regex("pattern\\.$SLUG")
        val SOUND = Regex("sound\\.$SLUG")
        val GRAMMAR = Regex("grammar\\.$SLUG")

        /** The ID part of a headword: `I'm` → `im`, `next to` → `next-to`. */
        fun slug(word: String): String =
            normalizeEnglish(word).replace("'", "").replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }
}
