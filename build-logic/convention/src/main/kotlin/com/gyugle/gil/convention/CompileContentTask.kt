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
package com.gyugle.gil.convention

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.NormalizeLineEndings

/**
 * Checks `content/` and builds the content database asset with tools/content-compiler.
 *
 * The compiler runs in its own JVM with only its own classpath: it loads SQLite's native library,
 * which must not end up in the Gradle daemon, and its kaml and serialization versions must not
 * mix with the ones on the build's plugin classpath.
 */
@CacheableTask
abstract class CompileContentTask : DefaultTask() {

    @get:Classpath
    abstract val compilerClasspath: ConfigurableFileCollection

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    abstract val contentDirectory: DirectoryProperty

    /** The committed Room schemas of the module; the newest version of its one database is used. */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:NormalizeLineEndings
    abstract val schemaDirectory: DirectoryProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val uiFonts: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val englishFonts: ConfigurableFileCollection

    /** Assets root. The database lands in `content/content.db` next to `content/manifest.json`. */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun compile() {
        val schemas = schemaDirectory.get().asFile.walkTopDown()
            .filter { it.isFile && it.extension == "json" && it.nameWithoutExtension.toIntOrNull() != null }
            .toList()
        val databases = schemas.map { it.parentFile }.distinct()
        if (databases.size != 1) {
            throw GradleException(
                "expected one Room database schema in ${schemaDirectory.get()}, found ${databases.size}"
            )
        }
        val schema = schemas.maxBy { it.nameWithoutExtension.toInt() }
        val out = outputDirectory.get().dir("content").asFile.apply { deleteRecursively() }
        val arguments = buildList {
            addAll(listOf("--content", contentDirectory.get().asFile.path))
            addAll(listOf("--schema", schema.path, "--out", out.path))
            addAll(listOf("--report", report.get().asFile.path))
            uiFonts.files.sorted().forEach { addAll(listOf("--ui-font", it.path)) }
            englishFonts.files.sorted().forEach { addAll(listOf("--english-font", it.path)) }
        }
        val scratch = temporaryDir.path
        val result = exec.javaexec {
            classpath = compilerClasspath
            mainClass.set("com.gyugle.gil.tools.content.MainKt")
            args = arguments
            // sqlite-jdbc unpacks its native library into the temporary directory, which a process
            // started by the daemon may not have.
            systemProperty("java.io.tmpdir", scratch)
            systemProperty("org.sqlite.tmpdir", scratch)
            isIgnoreExitValue = true
        }
        when (result.exitValue) {
            0 -> Unit
            1 -> throw GradleException("the learning content has errors; see the messages above")
            else -> throw GradleException("the content compiler failed with exit code ${result.exitValue}")
        }
    }
}
