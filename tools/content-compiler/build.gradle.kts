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
plugins {
    alias(libs.plugins.convention.jvm.library)
}

dependencies {
    implementation(libs.kaml)
    implementation(libs.sqlite.jdbc)
}

val fontDir: Directory = layout.settingsDirectory.dir("core/designsystem/src/main/res/font")

/** Runs the compiler on content/ without building a database. */
fun JavaExec.runOnContent(vararg extra: String) {
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "com.gyugle.gil.tools.content.MainKt"
    val content = layout.settingsDirectory.dir("content").asFile.path
    // The same font files the build hands to the compiler (gil.content).
    val files = fontDir.asFile.listFiles().orEmpty().sortedBy { it.name }
    val fonts = files.filter { it.name.startsWith("pretendard") }.flatMap { listOf("--ui-font", it.path) } +
        files.filter { it.name.startsWith("charis") }.flatMap { listOf("--english-font", it.path) }
    args(listOf("--content", content) + fonts + extra)
}

tasks.register<JavaExec>("checkContent") {
    group = "verification"
    description = "Checks the learning content in content/ and writes a report."
    runOnContent("--report", layout.buildDirectory.file("reports/content/report.md").get().asFile.path)
}

tasks.register<JavaExec>("lockContentIds") {
    group = "content"
    description = "Adds every content ID to content/ids.lock so it can never be changed or reused."
    runOnContent("--update-lock")
}

tasks.test {
    // The writer test builds a database from the real content schema.
    val schemaDir = layout.settingsDirectory.dir("feature/curriculum/impl/data/schemas")
    inputs.dir(schemaDir).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("gil.contentSchemaDir", schemaDir.asFile.path)
    inputs.dir(fontDir).withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("gil.fontDir", fontDir.asFile.path)
}
