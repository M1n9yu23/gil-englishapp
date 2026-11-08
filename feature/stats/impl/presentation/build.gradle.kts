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
    alias(libs.plugins.convention.android.feature)
    alias(libs.plugins.convention.android.screenshot)
}

dependencies {
    implementation(projects.feature.stats.api)
    implementation(projects.feature.stats.impl.domain)
    // The figures read the record and the course's stages; an empty record leads to the first lesson.
    implementation(projects.feature.progress.api)
    implementation(projects.feature.curriculum.api)
    implementation(projects.feature.lesson.api)
    implementation(libs.timber)

    testImplementation(testFixtures(projects.feature.curriculum.api))
    testImplementation(testFixtures(projects.feature.progress.api))
}
