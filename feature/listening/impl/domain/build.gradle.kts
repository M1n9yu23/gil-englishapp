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
    // Fakes of the player, the renderer and the preferences, for the screen's tests.
    `java-test-fixtures`
}

dependencies {
    // Playlists are made from the learner's lessons, schedule and dictionary.
    api(projects.feature.curriculum.api)
    api(projects.feature.progress.api)
    api(projects.core.domain)
    api(libs.kotlinx.coroutines.core)

    testImplementation(testFixtures(projects.feature.curriculum.api))
    testImplementation(testFixtures(projects.feature.progress.api))
}
