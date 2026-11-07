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
    alias(libs.plugins.convention.android.library)
    alias(libs.plugins.convention.hilt)
    alias(libs.plugins.convention.room)
}

android {
    // The migration tests read the committed schemas as assets, which Robolectric only sees
    // with Android resources included.
    sourceSets.getByName("test").assets.directories.add("$projectDir/schemas")
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(projects.feature.progress.api)
    implementation(projects.feature.progress.impl.domain)
    implementation(projects.core.database)

    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}
