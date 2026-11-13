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
    // The notification tests read the strings and the icon through Robolectric.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(projects.feature.reminder.api)
    implementation(projects.feature.reminder.impl.domain)
    implementation(projects.feature.goal.api)
    implementation(projects.feature.progress.api)
    // A reminder opens the Today tab.
    implementation(projects.feature.home.api)
    implementation(projects.core.datastore)

    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)

    testImplementation(testFixtures(projects.feature.goal.api))
    testImplementation(testFixtures(projects.feature.progress.api))
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}
