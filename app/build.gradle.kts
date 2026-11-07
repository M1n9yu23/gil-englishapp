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
import java.util.Properties

plugins {
    alias(libs.plugins.convention.android.application.compose)
    alias(libs.plugins.convention.hilt)
    alias(libs.plugins.convention.android.screenshot)
}

// Upload key settings stay out of git. Without the file, release builds come out unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val hasUploadKey = keystoreProperties.getProperty("KEYSTORE_PATH") != null

android {
    androidResources {
        // Lists en and ko for the per-app language setting; the default is in resources.properties.
        generateLocaleConfig = true
    }

    signingConfigs {
        if (hasUploadKey) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("KEYSTORE_PATH"))
                storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
                keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
                keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            optimization {
                enable = true
            }
            if (hasUploadKey) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        // Release code and R8, signed with the debug key, installable next to the other two.
        create("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            matchingFallbacks += "release"
            optimization {
                enable = true
            }
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.domain)
    implementation(projects.core.navigation)
    implementation(projects.core.speech)
    implementation(projects.feature.home.api)
    implementation(projects.feature.curriculum.api)
    implementation(projects.feature.curriculum.impl.presentation)
    implementation(projects.feature.curriculum.impl.data)
    implementation(projects.feature.progress.impl.data)
    implementation(projects.feature.lesson.api)
    implementation(projects.feature.lesson.impl.data)
    implementation(projects.feature.lesson.impl.presentation)
    implementation(projects.feature.goal.impl.data)
    implementation(projects.feature.reminder.impl.data)
    implementation(projects.feature.onboarding.api)
    implementation(projects.feature.onboarding.impl.data)
    implementation(projects.feature.onboarding.impl.presentation)
    implementation(projects.feature.dictionary.api)
    implementation(projects.feature.stats.api)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.timber)

    testImplementation(testFixtures(projects.feature.curriculum.api))
}
