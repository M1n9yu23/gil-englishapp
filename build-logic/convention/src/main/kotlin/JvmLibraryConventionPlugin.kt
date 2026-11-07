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
import com.android.build.api.dsl.Lint
import com.gyugle.gil.convention.configureKotlinJvmTarget
import com.gyugle.gil.convention.configureLint
import com.gyugle.gil.convention.configureUnitTests
import com.gyugle.gil.convention.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** For modules that touch no Android API: `core:domain`, feature `api` and `impl:domain` modules. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            // Android Lint for plain Kotlin modules, so the app's checkDependencies reaches them too.
            pluginManager.apply("com.android.lint")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            configureKotlinJvmTarget()
            extensions.configure<Lint> { configureLint() }
            configureUnitTests()
            // Android modules name their unit tests testDebugUnitTest; the same name here lets one
            // command run the tests of every module.
            tasks.register("testDebugUnitTest") {
                group = "verification"
                description = "Runs the unit tests (alias of test)."
                dependsOn("test")
            }

            dependencies {
                "implementation"(libs.findLibrary("kotlinx-coroutines-core").get())
                "implementation"(libs.findLibrary("kotlinx-serialization-json").get())
            }
        }
    }
}
