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

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

private const val ISSUE_FORMS = ".github/ISSUE_TEMPLATE/"

/** GitHub fills in only one pull request template, so it holds both languages in one file. */
private val SINGLE_FILE_BILINGUAL = setOf(".github/PULL_REQUEST_TEMPLATE.md")

/** How far below the title the link to the other language may sit. */
private const val SWITCH_LINK_LINES = 5

/**
 * Every public document ships in English (`name.md`) and Korean (`name.ko.md`). This checks that
 * each one has its partner and, for Markdown, links to it near the top. Issue forms pair the same
 * way (`name.yml`, `name.ko.yml`). Content and structure are left to review.
 */
@CacheableTask
abstract class VerifyDocTranslationsTask : DefaultTask() {
    /** Paths relative to the repository root, `/`-separated. */
    @get:Input
    abstract val documentPaths: ListProperty<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val documents: ConfigurableFileCollection

    @get:Internal
    abstract val repositoryRoot: DirectoryProperty

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val root = repositoryRoot.get().asFile
        val paths = documentPaths.get().toSet()
        val problems = paths.sorted().flatMap { path -> problemsFor(path, paths, root) }
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Each public document needs an English name.md and a Korean name.ko.md that link to " +
                    "each other within the first $SWITCH_LINK_LINES lines:\n" +
                    problems.joinToString("\n") { "  $it" }
            )
        }
        report.get().asFile.writeText("${paths.size} documents, all paired\n")
    }

    private fun problemsFor(path: String, paths: Set<String>, root: File): List<String> {
        if (path in SINGLE_FILE_BILINGUAL) return emptyList()
        val partner = partnerOf(path)
        if (partner !in paths) return listOf("$path has no $partner")
        if (!path.endsWith(".md")) return emptyList()

        val partnerName = partner.substringAfterLast('/')
        val top = File(root, path).useLines { lines -> lines.take(SWITCH_LINK_LINES).toList() }
        val linked = top.any { line -> "]($partnerName)" in line }
        return if (linked) emptyList() else listOf("$path does not link to $partnerName")
    }
}

internal fun isPublicDocument(path: String): Boolean = when {
    path.endsWith(".md") -> true

    path.startsWith(ISSUE_FORMS) -> {
        val name = path.removePrefix(ISSUE_FORMS)
        '/' !in name && (name.endsWith(".yml") || name.endsWith(".yaml")) && !name.startsWith("config.")
    }

    else -> false
}

/** `docs/build.md` <-> `docs/build.ko.md`, `bug_report.yml` <-> `bug_report.ko.yml`. */
internal fun partnerOf(path: String): String {
    val extension = path.substringAfterLast('.')
    val stem = path.removeSuffix(".$extension")
    return if (stem.endsWith(".ko")) "${stem.removeSuffix(".ko")}.$extension" else "$stem.ko.$extension"
}

/**
 * Registers `verifyDocTranslations` on the root project and hangs it on `check`. The file list comes
 * from git, so ignored local files never count as public documents.
 */
internal fun Project.configureDocTranslationCheck() {
    val repository = rootDir
    val paths = providers.exec {
        commandLine("git", "ls-files", "--cached", "--others", "--exclude-standard", "-z")
        workingDir = repository
    }.standardOutput.asText.map { output ->
        output.split('\u0000')
            .filter { it.isNotEmpty() && isPublicDocument(it) && File(repository, it).isFile }
            .sorted()
    }

    val verify = tasks.register<VerifyDocTranslationsTask>("verifyDocTranslations") {
        group = "verification"
        description = "Fails if a public document lacks its English or Korean partner."
        documentPaths.set(paths)
        documents.from(paths.map { list -> list.map { File(repository, it) } })
        repositoryRoot.set(layout.projectDirectory)
        report.set(layout.buildDirectory.file("reports/doc-translations.txt"))
    }
    tasks.named("check") { dependsOn(verify) }
}
