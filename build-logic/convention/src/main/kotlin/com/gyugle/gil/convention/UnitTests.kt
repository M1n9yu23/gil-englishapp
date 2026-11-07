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

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * JUnit Jupiter, AssertK and Turbine for local JVM tests.
 *
 * Neither AGP nor the Kotlin JVM plugin enables the JUnit Platform, and without
 * `useJUnitPlatform()` Gradle 9 fails a test task that discovers no tests.
 */
internal fun Project.configureUnitTests() {
    tasks.withType<Test>().configureEach { useJUnitPlatform() }
    dependencies {
        "testImplementation"(platform(libs.findLibrary("junit-bom").get()))
        "testImplementation"(libs.findLibrary("junit-jupiter").get())
        "testRuntimeOnly"(libs.findLibrary("junit-platform-launcher").get())
        "testImplementation"(libs.findLibrary("assertk").get())
        "testImplementation"(libs.findLibrary("turbine").get())
        "testImplementation"(libs.findLibrary("kotlinx-coroutines-test").get())
    }
}
