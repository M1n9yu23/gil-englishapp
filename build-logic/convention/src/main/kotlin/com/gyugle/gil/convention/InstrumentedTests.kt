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

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Device tests live in `src/androidTest`; a module without it gets no test APK and no test dependencies. */
internal val Project.hasDeviceTests: Boolean
    get() = projectDir.resolve("src/androidTest").exists()

/**
 * Device tests for any Android module.
 *
 * Libraries without `src/androidTest` get no test APK. Otherwise `connectedAndroidTest` installs an
 * empty one per module, and without a runner on its classpath the instrumentation crashes.
 * Test dependencies are added only where tests exist, because AGP warns about each dependency on a
 * disabled test component.
 * espresso-core is declared so Compose's ui-test cannot pull 3.5.0, which fails on API 36+ with
 * `NoSuchMethodException: android.hardware.input.InputManager.getInstance`.
 */
internal fun Project.configureInstrumentedTests() {
    pluginManager.withPlugin("com.android.library") {
        extensions.configure<LibraryAndroidComponentsExtension> {
            beforeVariants { variant ->
                variant.androidTest.enable = variant.androidTest.enable && hasDeviceTests
            }
        }
    }
    if (!hasDeviceTests) return
    dependencies {
        "androidTestImplementation"(libs.findLibrary("androidx-test-runner").get())
        "androidTestImplementation"(libs.findLibrary("androidx-test-ext-junit").get())
        "androidTestImplementation"(libs.findLibrary("androidx-test-espresso-core").get())
    }
}
