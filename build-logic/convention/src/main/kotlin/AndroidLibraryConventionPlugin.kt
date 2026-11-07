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
import com.android.build.api.dsl.LibraryExtension
import com.gyugle.gil.convention.catalogVersionInt
import com.gyugle.gil.convention.configureInstrumentedTests
import com.gyugle.gil.convention.configureKotlinJvmTarget
import com.gyugle.gil.convention.configureLint
import com.gyugle.gil.convention.configureUnitTests
import com.gyugle.gil.convention.libs
import com.gyugle.gil.convention.pathToPackageName
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** The base for every Android library module. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Not org.jetbrains.kotlin.android: AGP 9 registers the `kotlin` extension itself.
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<LibraryExtension> {
                namespace = pathToPackageName()
                compileSdk = catalogVersionInt("projectCompileSdkVersion")
                defaultConfig {
                    minSdk = catalogVersionInt("projectMinSdkVersion")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                lint { configureLint() }
            }
            configureKotlinJvmTarget()
            configureUnitTests()
            configureInstrumentedTests()

            dependencies {
                "implementation"(libs.findLibrary("kotlinx-coroutines-core").get())
                "implementation"(libs.findLibrary("kotlinx-serialization-json").get())
            }
        }
    }
}
