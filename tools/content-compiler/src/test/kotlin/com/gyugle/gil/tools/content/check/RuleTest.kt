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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import com.gyugle.gil.tools.content.CompileResult
import com.gyugle.gil.tools.content.SampleContent
import com.gyugle.gil.tools.content.errors
import com.gyugle.gil.tools.content.rules
import java.io.File
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** One broken copy of the sample course per test; the rule named by the test must catch it. */
class RuleTest {

    @TempDir
    lateinit var temp: File

    private fun sample() = SampleContent(File(temp, "content"))

    private fun assertCaught(result: CompileResult, rule: Rule) {
        assertThat(result.rules()).contains(rule)
    }

    private val lesson1 = "units/a1/a1-01.yaml"
    private val shadow = "      - type: shadow\n        en: I run every morning."

    @Test
    fun yamlInvalid_unknownKey_isReported() {
        assertCaught(sample().edit("course.yaml", "inflections:", "inflection:").compile(), Rule.YAML_INVALID)
    }

    @Test
    fun yamlInvalid_missingStepField_isReported() {
        assertCaught(
            sample().edit(
                lesson1,
                "        answers: [am]\n",
                ""
            ).edit(lesson1, "        en: I ___ a student.\n", "").compile(),
            Rule.YAML_INVALID
        )
    }

    @Test
    fun idFormat_wordIdDoesNotMatchHeadword_isReported() {
        assertCaught(
            sample().edit("lexicon/words.yaml", "id: word.morning", "id: word.mornin").compile(),
            Rule.ID_FORMAT
        )
    }

    @Test
    fun idFormat_senseIdHasWrongPartOfSpeech_isReported() {
        val result = sample()
            .edit("lexicon/words.yaml", "id: sense.morning.n.early-day", "id: sense.morning.v.early-day")
            .edit(lesson1, "sense.morning.n.early-day]", "sense.morning.v.early-day]")
            .edit(lesson1, "item: sense.morning.n.early-day", "item: sense.morning.v.early-day")
            .compile()

        assertCaught(result, Rule.ID_FORMAT)
    }

    @Test
    fun idDuplicate_twoPatternsShareAnId_isReported() {
        assertCaught(
            sample().edit("patterns.yaml", "id: pattern.past-simple", "id: pattern.nice-to-meet-you").compile(),
            Rule.ID_DUPLICATE
        )
    }

    @Test
    fun idPath_unitListedUnderAnotherStage_isReported() {
        val result = sample()
            .edit("course.yaml", "units: [unit.s1-01]", "units: [unit.s1-01, unit.a1-01]")
            .edit("course.yaml", "units: [unit.a1-01]", "units: []")
            .compile()

        assertCaught(result, Rule.ID_PATH)
    }

    @Test
    fun refMissing_lessonIntroducesUnknownSense_isReported() {
        assertCaught(
            sample().edit(lesson1, "introduces: [sense.run.v.move-fast", "introduces: [sense.run.v.walk").compile(),
            Rule.REF_MISSING
        )
    }

    @Test
    fun refUnlisted_unitFileNotInCourse_isReported() {
        val result = sample().write(
            "units/a1/a1-02.yaml",
            """
            id: unit.a1-02
            title: { ko: 둘째 단원, en: Second unit }
            lessons: []
            """
        ).compile()

        assertCaught(result, Rule.REF_UNLISTED)
    }

    @Test
    fun lockMissing_publishedIdIsGone_isReported() {
        assertCaught(sample().write("ids.lock", "sense.gone.n.away").compile(), Rule.LOCK_MISSING)
    }

    @Test
    fun lockMissing_retiredId_isNotReported() {
        assertThat(
            sample().write("ids.lock", "sense.gone.n.away retired").compile().rules()
        ).doesNotContain(Rule.LOCK_MISSING)
    }

    @Test
    fun lockReused_retiredIdComesBack_isReported() {
        assertCaught(sample().write("ids.lock", "sense.run.v.manage retired").compile(), Rule.LOCK_REUSED)
    }

    @Test
    fun introduceTwice_sameSenseInTwoLessons_isReported() {
        val result = sample().edit(
            lesson1,
            "introduces: [sense.run.v.move-fast",
            "introduces: [sense.i.pron.self, sense.run.v.move-fast"
        ).compile()

        assertCaught(result, Rule.INTRODUCE_TWICE)
    }

    @Test
    fun introduceLevel_levelDisagreesWithLessonStage_isReported() {
        val result = sample().edit(
            lesson1,
            "introduces: [sense.run.v.move-fast",
            "introduces: [sense.run.v.manage, sense.run.v.move-fast"
        ).compile()

        assertCaught(result, Rule.INTRODUCE_LEVEL)
    }

    @Test
    fun levelMissing_senseNeitherIntroducedNorLevelled_isReported() {
        assertCaught(sample().edit("lexicon/words.yaml", "      level: b1\n", "").compile(), Rule.LEVEL_MISSING)
    }

    @Test
    fun vocabUnknown_wordNotInLexicon_isReported() {
        assertCaught(
            sample().edit(
                lesson1,
                "      - type: dictation\n        en: I am a student.",
                "      - type: dictation\n        en: I am a doctor."
            ).compile(),
            Rule.VOCAB_UNKNOWN
        )
    }

    @Test
    fun vocabNotYet_wordTaughtInALaterLesson_isReported() {
        val result = sample().edit(
            lesson1,
            "      - type: dictation\n        en: I am a student.",
            "      - type: dictation\n        en: I run every morning."
        ).compile()

        assertCaught(result, Rule.VOCAB_NOT_YET)
    }

    @Test
    fun vocabNotYet_checkVocabOff_isNotReported() {
        val result = sample()
            .edit(
                lesson1,
                "      - type: dictation\n        en: I am a student.\n        ko: 저는 학생이에요.",
                "      - type: dictation\n        en: I run every morning.\n        ko: 저는 매일 아침 달려요.\n        checkVocab: false"
            )
            .compile()

        assertThat(result.errors).isEmpty()
    }

    @Test
    fun vocabNotYet_contractionEndingNotTaught_isReported() {
        assertCaught(
            sample().edit(
                lesson1,
                "      - type: dictation\n        en: I am a student.",
                "      - type: dictation\n        en: I'd run every morning."
            ).compile(),
            Rule.VOCAB_NOT_YET
        )
    }

    @Test
    fun vocabFormNotYet_pastFormBeforeItsPattern_isReported() {
        assertCaught(
            sample().edit(lesson1, shadow, "      - type: shadow\n        en: I ran every morning.").compile(),
            Rule.VOCAB_FORM_NOT_YET
        )
    }

    @Test
    fun vocabMark_markedSenseBelongsToAnotherWord_isReported() {
        val result = sample().edit(
            lesson1,
            shadow,
            "      - type: shadow\n        en: I {run|sense.morning.n.early-day} every morning."
        ).compile()

        assertCaught(result, Rule.VOCAB_MARK)
    }

    @Test
    fun exampleMissing_senseWithoutExample_isReported() {
        val result = sample().write(
            "lexicon/extra.yaml",
            """
            - word: book
              id: word.book
              senses:
                - id: sense.book.n.pages
                  pos: noun
                  ko: 책
                  level: a1
            """
        ).compile()

        assertCaught(result, Rule.EXAMPLE_MISSING)
    }

    @Test
    fun textEmpty_blankChoice_isReported() {
        assertCaught(
            sample().edit(lesson1, "{ ko: 학생, correct: true }", "{ ko: \" \", correct: true }").compile(),
            Rule.TEXT_EMPTY
        )
    }

    @Test
    fun textLength_choiceOverFortyCharacters_isReported() {
        val long = "아주 오래전부터 알고 지낸 가까운 사이의 친구나 같은 반에서 함께 공부하는 친구"
        assertCaught(sample().edit(lesson1, "{ ko: 친구, why", "{ ko: $long, why").compile(), Rule.TEXT_LENGTH)
    }

    @Test
    fun fontGlyph_emojiInKoreanText_isReported() {
        val result = sample().edit(lesson1, "title: { ko: 저는 학생이에요,", "title: { ko: 저는 학생이에요 😀,").compile(fonts = true)

        assertCaught(result, Rule.FONT_GLYPH)
    }

    @Test
    fun fontGlyph_hangulSyllables_isNotReported() {
        assertThat(sample().compile(fonts = true).rules()).doesNotContain(Rule.FONT_GLYPH)
    }

    @Test
    fun markupInvalid_unknownChunkRole_isReported() {
        assertCaught(sample().edit(lesson1, "[run]{do}", "[run]{verb}").compile(), Rule.MARKUP_INVALID)
    }

    @Test
    fun markupInvalid_unclosedBacktick_isReported() {
        assertCaught(sample().edit(lesson1, "`I run`처럼", "`I run처럼").compile(), Rule.MARKUP_INVALID)
    }

    @Test
    fun ipaMissing_soundExampleNotInCmudict_isReported() {
        assertCaught(
            sample().edit("sounds.yaml", "examples: [ship, sit]", "examples: [ship, zzxq]").compile(),
            Rule.IPA_MISSING
        )
    }

    @Test
    fun stepOptions_twoCorrectChoices_isReported() {
        assertCaught(
            sample().edit(lesson1, "{ ko: 친구, why: 친구는 `friend`예요. }", "{ ko: 친구, correct: true }").compile(),
            Rule.STEP_OPTIONS
        )
    }

    @Test
    fun stepOptions_englishChoiceWhereKoreanIsExpected_isReported() {
        assertCaught(
            sample().edit(lesson1, "{ ko: 친구, why: 친구는 `friend`예요. }", "{ en: student, why: 뜻을 고르세요. }").compile(),
            Rule.STEP_OPTIONS
        )
    }

    @Test
    fun stepWhy_wrongChoiceWithoutReason_isReported() {
        assertCaught(
            sample().edit(lesson1, "{ ko: 선생님, why: 선생님은 `teacher`예요. }", "{ ko: 선생님 }").compile(),
            Rule.STEP_WHY
        )
    }

    @Test
    fun stepBlank_noBlank_isReported() {
        assertCaught(sample().edit(lesson1, "en: I ___ a student.", "en: I am a student!").compile(), Rule.STEP_BLANK)
    }

    @Test
    fun stepTiles_acceptedOrderUsesOtherWords_isReported() {
        val result = sample().edit(
            lesson1,
            "        en: I am a student.\n      - type: fillBlank",
            "        en: I am a student.\n        accept: [I am student.]\n      - type: fillBlank"
        ).compile()

        assertCaught(result, Rule.STEP_TILES)
    }

    @Test
    fun stepItem_itemIsNotAStudyItem_isReported() {
        assertCaught(
            sample().edit(lesson1, "item: sense.student.n.learner", "item: word.student").compile(),
            Rule.STEP_ITEM
        )
    }

    @Test
    fun schemaMismatch_compilerWritesColumnTheSchemaLacks_isReported() {
        val schema = File(temp, "schema.json").apply {
            writeText(
                SampleContent.schemaFile.readText().replace(
                    "\"columnName\": \"searchKey\"",
                    "\"columnName\": \"searchTerms\""
                )
            )
        }

        assertCaught(sample().compile(schema = schema, out = File(temp, "out")), Rule.SCHEMA_MISMATCH)
    }

    @Test
    fun styleKo_misspelling_isReported() {
        assertCaught(
            sample().edit(lesson1, "goal: { ko: 매일 하는 일을 말할 수 있어요", "goal: { ko: 매일 하는 일을 말하게 되요").compile(),
            Rule.STYLE_KO
        )
    }

    @Test
    fun styleEn_sentenceWithoutClosingPunctuation_isReported() {
        assertCaught(
            sample().edit(lesson1, shadow, "      - type: shadow\n        en: I run every morning").compile(),
            Rule.STYLE_EN
        )
    }

    @Test
    fun textLatin_englishOutsideBackticksInExplanation_isReported() {
        assertCaught(sample().edit(lesson1, "`I run`처럼", "I run처럼").compile(), Rule.TEXT_LATIN)
    }

    @Test
    fun senseAmbiguous_twoTaughtSensesAndNoMark_isReported() {
        val result = sample()
            .edit("lexicon/words.yaml", "      level: b1\n", "")
            .edit(
                lesson1,
                "introduces: [sense.run.v.move-fast",
                "introduces: [sense.run.v.manage, sense.run.v.move-fast"
            )
            .compile()

        assertCaught(result, Rule.SENSE_AMBIGUOUS)
    }

    @Test
    fun ttsSymbol_ampersandInSpokenText_isReported() {
        assertCaught(
            sample().edit(lesson1, shadow, "      - type: shadow\n        en: I run & run every morning.").compile(),
            Rule.TTS_SYMBOL
        )
    }

    @Test
    fun ipaVariants_severalPronunciationsAndNoChoice_isReported() {
        assertCaught(sample().compile(), Rule.IPA_VARIANTS)
    }

    @Test
    fun ipaVariants_explicitIpa_isNotReported() {
        val result = sample().edit(
            "lexicon/words.yaml",
            "- word: every\n  id: word.every\n",
            "- word: every\n  id: word.every\n  ipa: ˈɛvri\n"
        ).compile()

        assertThat(result.rules()).doesNotContain(Rule.IPA_VARIANTS)
    }

    @Test
    fun everyRule_inRuleEnum_hasAFailingTest() {
        val tested = RuleTest::class.java.declaredMethods.map { it.name.substringBefore('_').lowercase() }.toSet()
        val untested = Rule.entries.filter { rule -> rule.name.replace("_", "").lowercase() !in tested }

        assertThat(untested).isEmpty()
    }
}
