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
import com.android.build.api.dsl.CommonExtension
import com.gyugle.gil.convention.libs
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Roborazzi screenshot tests on the JVM through Robolectric. Apply after an Android Compose plugin.
 *
 * Baselines are committed under `src/test/screenshots`. Robolectric runs on JUnit 4, so the vintage
 * engine is required; without it the tests are silently skipped.
 */
class AndroidScreenshotConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.github.takahirom.roborazzi")

            extensions.configure(CommonExtension::class.java) {
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            extensions.configure<RoborazziExtension> {
                outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
            }

            dependencies {
                "testImplementation"(platform(libs.findLibrary("androidx-compose-bom").get()))
                "testImplementation"(libs.findLibrary("androidx-compose-ui-test-junit4").get())
                "testImplementation"(libs.findLibrary("robolectric").get())
                "testImplementation"(libs.findLibrary("roborazzi").get())
                "testImplementation"(libs.findLibrary("roborazzi-compose").get())
                "testImplementation"(libs.findLibrary("roborazzi-junit-rule").get())
                "testImplementation"(libs.findLibrary("junit4").get())
                "testRuntimeOnly"(libs.findLibrary("junit-vintage-engine").get())
                "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
            }
        }
    }
}
