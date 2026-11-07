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
package com.gyugle.gil.tools.content.report

import com.gyugle.gil.tools.content.check.Issue
import com.gyugle.gil.tools.content.check.Severity
import com.gyugle.gil.tools.content.compile.LessonSummary

/**
 * The Markdown report: problems first, then every lesson in course order with the items it
 * introduces. Authors read it to see what the learner already knows before writing a new unit.
 */
object ContentReport {

    fun render(issues: List<Issue>, lessons: List<LessonSummary>, counts: Map<String, Int>): String = buildString {
        appendLine("# Content report")
        appendLine()
        appendLine(counts.entries.joinToString(", ") { "${it.value} ${it.key}" })
        appendLine()
        for (severity in Severity.entries) {
            val list = issues.filter { it.severity == severity }
            appendLine("## ${if (severity == Severity.ERROR) "Errors" else "Warnings"} (${list.size})")
            appendLine()
            list.forEach { appendLine("- `${it.rule.id}` ${it.where}: ${it.message}") }
            if (list.isNotEmpty()) appendLine()
        }
        appendLine("## Lessons")
        var unit = ""
        var known = 0
        for (lesson in lessons) {
            if (lesson.unitId != unit) {
                unit = lesson.unitId
                appendLine()
                appendLine("### $unit (${lesson.stageId})")
                appendLine()
            }
            known += lesson.introduces.size
            val unchecked = if (lesson.uncheckedSteps > 0) ", ${lesson.uncheckedSteps} without vocabulary check" else ""
            appendLine("- ${lesson.id} ${lesson.title}: ${lesson.steps} steps$unchecked, $known items known after it")
            lesson.introduces.forEach { appendLine("  - $it") }
        }
    }
}
