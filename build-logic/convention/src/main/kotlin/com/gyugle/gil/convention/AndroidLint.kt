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

import com.android.build.api.dsl.Lint

/** Checks that only report how old a dependency is. Dependabot owns version updates. */
private val VERSION_CHECKS = setOf(
    "GradleDependency",
    "NewerVersionAvailable",
    "AndroidGradlePluginVersion"
)

/**
 * Text, accessibility and resource checks that are off or only warnings by default. Every string a
 * learner sees must be translated into both languages and read well at large font scales.
 */
private val RAISED_TO_ERROR = setOf(
    "HardcodedText",
    "ContentDescription",
    "SmallSp",
    "SpUsage",
    "TypographyEllipsis",
    "TypographyDashes",
    "UnusedResources",
    "MissingTranslation"
)

/** Lint settings shared by every Android module: a warning fails the build like an error does. */
internal fun Lint.configureLint() {
    abortOnError = true
    warningsAsErrors = true
    disable += VERSION_CHECKS
    enable += RAISED_TO_ERROR
    error += RAISED_TO_ERROR
}
