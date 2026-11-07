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

import com.gyugle.gil.tools.content.check.Severity
import java.io.File
import kotlin.system.exitProcess

/**
 * Command line:
 * `--content <dir> [--schema <room schema json> --out <dir>] [--report <file>]
 * [--ui-font <file>]... [--english-font <file>]... [--update-lock]`
 */
fun main(args: Array<String>) {
    val code = ContentCompilerCli.run(args)
    if (code != 0) exitProcess(code)
}

object ContentCompilerCli {

    /** Runs the compiler and prints the issues. Returns 0 on success, 1 on content errors, 2 on bad arguments. */
    @JvmStatic
    fun run(args: Array<String>): Int {
        val options = parse(args) ?: return 2
        val result = ContentCompiler().compile(options)
        result.issues.sortedBy { it.severity }.forEach(::println)
        val errors = result.issues.count { it.severity == Severity.ERROR }
        val warnings = result.issues.size - errors
        println("content: $errors errors, $warnings warnings" + (result.revision?.let { ", revision $it" } ?: ""))
        options.report?.let { println("report: ${it.path}") }
        return if (result.failed) 1 else 0
    }

    private fun parse(args: Array<String>): CompileOptions? {
        var content: File? = null
        var schema: File? = null
        var out: File? = null
        var report: File? = null
        val uiFonts = mutableListOf<File>()
        val englishFonts = mutableListOf<File>()
        var updateLock = false
        var i = 0
        fun value(): File? = args.getOrNull(++i)?.let(::File)
        while (i < args.size) {
            when (args[i]) {
                "--content" -> content = value()
                "--schema" -> schema = value()
                "--out" -> out = value()
                "--report" -> report = value()
                "--ui-font" -> value()?.let(uiFonts::add)
                "--english-font" -> value()?.let(englishFonts::add)
                "--update-lock" -> updateLock = true
                else -> return usage("unknown argument ${args[i]}")
            }
            i++
        }
        if (content == null) return usage("--content is required")
        if ((schema == null) != (out == null)) return usage("--schema and --out go together")
        return CompileOptions(content, schema, out, report, uiFonts, englishFonts, updateLock)
    }

    private fun usage(problem: String): CompileOptions? {
        System.err.println("content compiler: $problem")
        return null
    }
}
