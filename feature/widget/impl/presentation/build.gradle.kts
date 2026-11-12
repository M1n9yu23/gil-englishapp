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
}

android {
    // The widget tests render the strings through Robolectric.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    implementation(projects.feature.widget.impl.domain)
    // The widget reads the record and the dictionary, and opens reviews, sentences and Today.
    implementation(projects.feature.progress.api)
    implementation(projects.feature.curriculum.api)
    implementation(projects.feature.home.api)
    implementation(projects.feature.review.api)
    implementation(projects.feature.dictionary.api)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    // Glance runs its sessions on WorkManager and asks for 2.7; the newer one knows recent Android.
    implementation(libs.androidx.work.runtime)
    // The widget's Glance state, which tells a running session to read the day again.
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)

    testImplementation(libs.androidx.glance.appwidget.testing)
    testImplementation(libs.robolectric)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.junit.vintage.engine)
}
