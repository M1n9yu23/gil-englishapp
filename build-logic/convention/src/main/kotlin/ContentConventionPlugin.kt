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
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.gyugle.gil.convention.CompileContentTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

/**
 * Builds the content database from `content/` for every variant and adds it to the variant's
 * assets. Applied by the module that owns the content database, after gil.room.
 */
class ContentConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val compiler = configurations.create("contentCompiler") {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
            }
            dependencies { compiler(project(":tools:content-compiler")) }
            val fonts = layout.settingsDirectory.dir("core/designsystem/src/main/res/font")

            extensions.getByType(LibraryAndroidComponentsExtension::class.java).onVariants { variant ->
                val name = variant.name.replaceFirstChar(Char::uppercaseChar)
                val compile = tasks.register<CompileContentTask>("compile${name}Content") {
                    group = "build"
                    description = "Checks content/ and builds the content database for ${variant.name}."
                    // Room writes a schema to KSP's output only when it differs from the committed
                    // copy in schemas/ (gil.room), so schemas/ is the directory that is always complete.
                    dependsOn("copyRoomSchemas", "ksp${name}Kotlin")
                    schemaDirectory.set(layout.projectDirectory.dir("schemas"))
                    compilerClasspath.from(compiler)
                    contentDirectory.set(layout.settingsDirectory.dir("content"))
                    uiFonts.from(fonts.asFileTree.matching { include("pretendard*") })
                    englishFonts.from(fonts.asFileTree.matching { include("charis*") })
                    outputDirectory.set(layout.buildDirectory.dir("generated/content/${variant.name}"))
                    report.set(layout.buildDirectory.file("reports/content/${variant.name}.md"))
                }
                variant.sources.assets?.addGeneratedSourceDirectory(compile, CompileContentTask::outputDirectory)
            }
        }
    }
}
