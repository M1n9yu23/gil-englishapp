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
}

android {
    // The renderer and service tests read files and strings through Robolectric.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.feature.listening.api)
    implementation(projects.feature.listening.impl.domain)
    implementation(projects.core.speech)
    implementation(projects.core.datastore)
    // Listening time counts as study time.
    implementation(projects.feature.progress.api)
    // English is spoken with the accent chosen in the settings.
    implementation(projects.feature.settings.api)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.timber)

    testImplementation(testFixtures(projects.core.speech))
    testImplementation(testFixtures(projects.feature.settings.api))
    testImplementation(testFixtures(projects.feature.progress.api))
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}
