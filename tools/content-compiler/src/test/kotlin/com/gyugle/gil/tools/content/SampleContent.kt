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
import com.gyugle.gil.tools.content.check.Rule
import com.gyugle.gil.tools.content.check.Severity
import com.gyugle.gil.tools.content.pronunciation.Cmudict
import java.io.File

/**
 * The small course in `src/test/resources/sample`, copied to a temporary directory so a test can
 * break one thing and see which rule catches it.
 */
class SampleContent(val dir: File) {

    init {
        File(
            checkNotNull(SampleContent::class.java.getResource("/sample")).toURI()
        ).copyRecursively(dir, overwrite = true)
    }

    /** Replaces the only occurrence of [old] in [path]. Fails when [old] is missing or repeated. */
    fun edit(path: String, old: String, new: String): SampleContent = apply {
        val file = File(dir, path)
        val text = file.readText()
        val count = text.windowed(old.length).count { it == old }
        check(count == 1) { "'$old' occurs $count times in $path" }
        file.writeText(text.replace(old, new))
    }

    fun write(path: String, text: String): SampleContent = apply {
        File(dir, path).apply { parentFile.mkdirs() }.writeText(text.trimIndent() + "\n")
    }

    fun compile(
        schema: File? = null,
        out: File? = null,
        fonts: Boolean = false,
        updateLock: Boolean = false,
        report: File? = null
    ): CompileResult = compiler.compile(
        CompileOptions(
            contentDir = dir,
            schema = schema,
            outputDir = out,
            report = report,
            uiFonts = if (fonts) fontFiles("pretendard") else emptyList(),
            englishFonts = if (fonts) fontFiles("charis") else emptyList(),
            updateLock = updateLock
        )
    )

    companion object {
        /** Loading CMUdict takes a moment, so every test shares one copy. */
        val cmudict by lazy { Cmudict.bundled() }
        val compiler by lazy { ContentCompiler(cmudict) }

        val schemaFile: File by lazy {
            val dir =
                File(
                    System.getProperty("gil.contentSchemaDir"),
                    "com.gyugle.gil.feature.curriculum.impl.data.ContentDatabase"
                )
            checkNotNull(
                dir.listFiles { f ->
                    f.extension == "json"
                }?.maxByOrNull { it.nameWithoutExtension.toInt() }
            ) {
                "no content schema in $dir"
            }
        }

        fun fontFiles(prefix: String): List<File> = File(System.getProperty("gil.fontDir")).listFiles { f ->
            f.name.startsWith(prefix)
        }.orEmpty().sortedBy { it.name }
    }
}

val CompileResult.errors: List<Issue> get() = issues.filter { it.severity == Severity.ERROR }

fun CompileResult.rules(): Set<Rule> = issues.map { it.rule }.toSet()
