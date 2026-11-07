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
import com.gyugle.gil.tools.content.SampleContent
import com.gyugle.gil.tools.content.errors
import com.gyugle.gil.tools.content.rules
import java.io.File
import java.sql.DriverManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

/** Branches of the vocabulary check, each run on the shadowing step of the sample's last lesson. */
class VocabularyTest {

    @TempDir
    lateinit var temp: File

    private val lesson = "units/a1/a1-01.yaml"
    private val shadow = "      - type: shadow\n        en: I run every morning."

    private fun withShadow(sentence: String) =
        SampleContent(File(temp, "content")).edit(lesson, shadow, "      - type: shadow\n        en: \"$sentence\"")

    private fun SampleContent.gate(kind: String) =
        edit("course.yaml", "  past: pattern.past-simple", "  past: pattern.past-simple\n  $kind: pattern.past-simple")

    @Test
    fun possessive_nounBeforeItsGate_isReported() {
        assertThat(
            withShadow("A student's morning.").gate("possessive").compile().rules()
        ).contains(Rule.VOCAB_FORM_NOT_YET)
    }

    @Test
    fun possessive_nounWithoutGate_isAccepted() {
        assertThat(withShadow("A student's morning.").compile().errors).isEmpty()
    }

    @Test
    fun possessive_properNounBeforeItsGate_isReported() {
        assertThat(
            withShadow("I am @Mina's student.").gate("possessive").compile().rules()
        ).contains(Rule.VOCAB_FORM_NOT_YET)
    }

    @Test
    fun third_formBeforeItsGate_isReported() {
        assertThat(
            withShadow("@Mina runs every morning.").gate("third").compile().rules()
        ).contains(Rule.VOCAB_FORM_NOT_YET)
    }

    @Test
    fun third_formWithoutGate_isAccepted() {
        assertThat(withShadow("@Mina runs every morning.").compile().errors).isEmpty()
    }

    @Test
    fun senseMark_narrowerThanTheWord_isReported() {
        assertThat(withShadow("I am {run|sense.run.v.move-fast}ning.").compile().rules()).contains(Rule.MARKUP_INVALID)
    }

    @Test
    fun name_withoutAtSign_isUnknown() {
        assertThat(withShadow("I run every morning, Mina.").compile().rules()).contains(Rule.VOCAB_UNKNOWN)
    }

    @Test
    fun number_digits_areSkipped() {
        assertThat(withShadow("I am 20.").compile().errors).isEmpty()
    }

    @Test
    fun patternWords_withoutThePattern_areUnknown() {
        val result = SampleContent(File(temp, "content"))
            .edit(lesson, "sense.student.n.learner, pattern.nice-to-meet-you]", "sense.student.n.learner]")
            .compile()

        assertThat(result.rules()).contains(Rule.VOCAB_UNKNOWN)
    }

    @Test
    fun phrase_taughtHeadword_linksEveryWordOfIt() {
        val out = File(temp, "out")
        SampleContent(File(temp, "content"))
            .write(
                "lexicon/phrases.yaml",
                """
                - word: every morning
                  id: word.every-morning
                  senses:
                    - id: sense.every-morning.adv.daily
                      pos: adverb
                      ko: 매일 아침
                      examples:
                        - en: I run every morning.
                          ko: 저는 매일 아침 달려요.
                """
            )
            .edit(lesson, "sense.morning.n.early-day]\n", "sense.morning.n.early-day, sense.every-morning.adv.daily]\n")
            .compile(schema = SampleContent.schemaFile, out = out)

        DriverManager.getConnection("jdbc:sqlite:${File(out, "content.db").absolutePath}").use { db ->
            val tokens = db.createStatement().executeQuery(
                "SELECT tokens FROM sentence WHERE text = 'I run every morning.' AND chunks IS NULL"
            )
                .use { generateSequence { if (it.next()) it.getString(1) else null }.toList() }
            assertThat(tokens.joinToString()).contains(""""start":6,"end":11,"word":"word.every-morning"""")
        }
    }

    @Test
    fun phrase_notTaught_fallsBackToSingleWords() {
        val result = SampleContent(File(temp, "content")).write(
            "lexicon/phrases.yaml",
            """
            - word: every morning
              id: word.every-morning
              senses:
                - id: sense.every-morning.adv.daily
                  pos: adverb
                  ko: 매일 아침
                  level: a2
                  examples:
                    - en: I run every morning.
                      ko: 저는 매일 아침 달려요.
            """
        ).compile()

        assertThat(result.rules()).doesNotContain(Rule.VOCAB_NOT_YET)
    }
}
