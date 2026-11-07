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

enum class Severity { ERROR, WARNING }

/** Every rule the compiler checks. Errors fail the build; warnings go to the report. */
enum class Rule(val id: String, val severity: Severity) {
    YAML_INVALID("yaml.invalid", Severity.ERROR),
    ID_FORMAT("id.format", Severity.ERROR),
    ID_DUPLICATE("id.duplicate", Severity.ERROR),
    ID_PATH("id.path", Severity.ERROR),
    REF_MISSING("ref.missing", Severity.ERROR),
    REF_UNLISTED("ref.unlisted", Severity.ERROR),
    LOCK_MISSING("lock.missing", Severity.ERROR),
    LOCK_REUSED("lock.reused", Severity.ERROR),
    INTRODUCE_TWICE("introduce.twice", Severity.ERROR),
    INTRODUCE_LEVEL("introduce.level", Severity.ERROR),
    LEVEL_MISSING("level.missing", Severity.ERROR),
    VOCAB_UNKNOWN("vocab.unknown", Severity.ERROR),
    VOCAB_NOT_YET("vocab.not-yet", Severity.ERROR),
    VOCAB_FORM_NOT_YET("vocab.form-not-yet", Severity.ERROR),
    VOCAB_MARK("vocab.mark", Severity.ERROR),
    EXAMPLE_MISSING("example.missing", Severity.ERROR),
    TEXT_EMPTY("text.empty", Severity.ERROR),
    TEXT_LENGTH("text.length", Severity.ERROR),
    FONT_GLYPH("font.glyph", Severity.ERROR),
    MARKUP_INVALID("markup.invalid", Severity.ERROR),
    IPA_MISSING("ipa.missing", Severity.ERROR),
    STEP_OPTIONS("step.options", Severity.ERROR),
    STEP_WHY("step.why", Severity.ERROR),
    STEP_BLANK("step.blank", Severity.ERROR),
    STEP_TILES("step.tiles", Severity.ERROR),
    STEP_ITEM("step.item", Severity.ERROR),
    SCHEMA_MISMATCH("schema.mismatch", Severity.ERROR),
    STYLE_KO("style.ko", Severity.WARNING),
    STYLE_EN("style.en", Severity.WARNING),
    TEXT_LATIN("text.latin", Severity.WARNING),
    SENSE_AMBIGUOUS("sense.ambiguous", Severity.WARNING),
    TTS_SYMBOL("tts.symbol", Severity.WARNING),
    IPA_VARIANTS("ipa.variants", Severity.WARNING)
}

/** [where] reads like `units/s1/s1-04.yaml › lesson.s1-04.1 › step 3`. */
data class Issue(val rule: Rule, val where: String, val message: String) {
    val severity: Severity get() = rule.severity

    override fun toString(): String = "${severity.name.lowercase()} [${rule.id}] $where: $message"
}

class Issues {
    private val list = mutableListOf<Issue>()

    val all: List<Issue> get() = list

    val hasErrors: Boolean get() = list.any { it.severity == Severity.ERROR }

    fun add(rule: Rule, where: String, message: String) {
        list += Issue(rule, where, message)
    }
}

/** Joins location parts with the separator used in every message. */
fun at(vararg parts: String): String = parts.filter { it.isNotEmpty() }.joinToString(" › ")
