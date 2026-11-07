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
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.spotless)
}

// One Spotless setup at the root covers every module and the included build-logic build.
// ktlint and compose-rules settings come from .editorconfig.
spotless {
    // .gitattributes already forces LF. Reading it per file would also hash locked files under .gradle.
    lineEndings = com.diffplug.spotless.LineEnding.UNIX
    val ktlintVersion = libs.versions.ktlint.get()
    val composeRules = libs.compose.rules.ktlint.get().let { "${it.module}:${it.versionConstraint.requiredVersion}" }
    val licenseHeader = rootProject.file("spotless/license-header.txt")
    // Build output and hidden tool folders at the root (.gradle, .idea, .kotlin and similar).
    val excluded = listOf("**/build/**", ".*/**")

    kotlin {
        target("**/*.kt")
        targetExclude(excluded)
        ktlint(ktlintVersion).customRuleSets(listOf(composeRules))
        licenseHeaderFile(licenseHeader)
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude(excluded)
        ktlint(ktlintVersion)
        // A .kts file has no package line, so the header ends at the first line that is not a block comment.
        licenseHeaderFile(licenseHeader, "(^(?![\\/ ]\\*).*$)")
    }
}
