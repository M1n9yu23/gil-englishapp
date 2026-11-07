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
package com.gyugle.gil.tools.content

import com.gyugle.gil.tools.content.check.Issue
import com.gyugle.gil.tools.content.check.Issues
import com.gyugle.gil.tools.content.check.Severity
import com.gyugle.gil.tools.content.compile.CompiledContent
import com.gyugle.gil.tools.content.compile.ContentAnalyzer
import com.gyugle.gil.tools.content.database.ContentDatabaseWriter
import com.gyugle.gil.tools.content.database.RoomSchema
import com.gyugle.gil.tools.content.font.FontCoverage
import com.gyugle.gil.tools.content.pronunciation.Cmudict
import com.gyugle.gil.tools.content.report.ContentReport
import com.gyugle.gil.tools.content.source.ContentLoader
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * What to compile. Without [schema] and [outputDir] only the checks and the report run.
 *
 * [uiFonts] are the files of the app's text font, [englishFonts] those of the English font.
 */
data class CompileOptions(
    val contentDir: File,
    val schema: File? = null,
    val outputDir: File? = null,
    val report: File? = null,
    val uiFonts: List<File> = emptyList(),
    val englishFonts: List<File> = emptyList(),
    val updateLock: Boolean = false
)

class CompileResult(val issues: List<Issue>, val content: CompiledContent?, val revision: String?) {
    val failed: Boolean get() = issues.any { it.severity == Severity.ERROR }
}

/** Loads, checks and, when asked, writes `content.db` and `manifest.json` into the output directory. */
class ContentCompiler(private val cmudict: Cmudict = Cmudict.bundled()) {

    fun compile(options: CompileOptions): CompileResult {
        val issues = Issues()
        val loaded = ContentLoader(options.contentDir, issues).load()
        val analyzer = ContentAnalyzer(
            loaded,
            cmudict,
            issues,
            options.uiFonts.takeIf { it.isNotEmpty() }?.let(FontCoverage::of),
            options.englishFonts.takeIf { it.isNotEmpty() }?.let(FontCoverage::of)
        )
        val content = analyzer.analyze()
        var revision: String? = null
        if (!issues.hasErrors && options.schema != null && options.outputDir != null) {
            val schema = RoomSchema.parse(options.schema.readText())
            val writer = ContentDatabaseWriter(schema, issues)
            if (writer.write(content.rows, File(options.outputDir, DATABASE))) {
                revision = writer.revision(content.rows)
                File(options.outputDir, MANIFEST).writeText(manifest(revision, schema.identityHash, counts(content)))
            }
        }
        if (options.updateLock && !issues.hasErrors) writeLock(options.contentDir, loaded.lock.retired, content.ids)
        options.report?.let { report ->
            report.parentFile?.mkdirs()
            report.writeText(ContentReport.render(issues.all, content.lessons, counts(content)))
        }
        return CompileResult(issues.all, content, revision)
    }

    private fun counts(content: CompiledContent): Map<String, Int> = linkedMapOf(
        "stages" to content.rows.count("stage"),
        "units" to content.rows.count("unit"),
        "lessons" to content.rows.count("lesson"),
        "words" to content.rows.count("word"),
        "senses" to content.rows.count("sense"),
        "patterns" to content.rows.count("pattern"),
        "sounds" to content.rows.count("sound"),
        "sentences" to content.rows.count("sentence")
    )

    private fun manifest(revision: String, identityHash: String, counts: Map<String, Int>): String {
        val json = buildJsonObject {
            put("revision", revision)
            put("schemaIdentityHash", identityHash)
            put("counts", buildJsonObject { counts.forEach { (k, v) -> put(k, JsonPrimitive(v)) } })
        }
        return ManifestJson.encodeToString(json) + "\n"
    }

    /** Adds every current ID to `ids.lock`, keeping retired ones. The file stays sorted. */
    private fun writeLock(contentDir: File, retired: Set<String>, current: Set<String>) {
        val file = File(contentDir, ContentLoader.LOCK)
        val existing = if (file.isFile) {
            file.readLines().map {
                it.substringBefore('#').trim()
            }.filter { it.isNotEmpty() }.map { it.split(Regex("\\s+")).first() }
        } else {
            emptyList()
        }
        val lines = (existing + current).toSortedSet().map { if (it in retired) "$it ${ContentLoader.RETIRED}" else it }
        file.writeText(LOCK_HEADER + lines.joinToString("\n", postfix = "\n"))
    }

    companion object {
        private val ManifestJson = Json { prettyPrint = true }
        const val DATABASE = "content.db"
        const val MANIFEST = "manifest.json"
        private const val LOCK_HEADER =
            "# Published content IDs. Never edit or remove one; mark a removed ID with \"retired\".\n" +
                "# Updated by ./gradlew lockContentIds\n"
    }
}
