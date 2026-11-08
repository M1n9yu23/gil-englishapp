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
    implementation(projects.feature.onboarding.api)
    implementation(projects.feature.onboarding.impl.domain)
    // The first run sets the daily goal and the reminder times, and checks the voice.
    implementation(projects.feature.goal.api)
    implementation(projects.feature.reminder.api)
    implementation(projects.core.speech)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    testImplementation(testFixtures(projects.core.speech))
    testImplementation(testFixtures(projects.feature.goal.api))
    testImplementation(testFixtures(projects.feature.reminder.api))
}
