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
import com.android.build.api.dsl.ApplicationExtension
import com.gyugle.gil.convention.ROOT_PACKAGE
import com.gyugle.gil.convention.catalogVersion
import com.gyugle.gil.convention.catalogVersionInt
import com.gyugle.gil.convention.configureInstrumentedTests
import com.gyugle.gil.convention.configureKotlinJvmTarget
import com.gyugle.gil.convention.configureNoInternetPermissionCheck
import com.gyugle.gil.convention.configureUnitTests
import com.gyugle.gil.convention.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Application modules: identity and SDK levels come from the catalog, and every variant is checked
 * for the INTERNET permission. Build types and signing stay in the module.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            extensions.configure<ApplicationExtension> {
                namespace = ROOT_PACKAGE
                compileSdk = catalogVersionInt("projectCompileSdkVersion")
                defaultConfig {
                    applicationId = catalogVersion("projectApplicationId")
                    minSdk = catalogVersionInt("projectMinSdkVersion")
                    targetSdk = catalogVersionInt("projectTargetSdkVersion")
                    versionCode = catalogVersionInt("projectVersionCode")
                    versionName = catalogVersion("projectVersionName")
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                // AGP 9 no longer generates BuildConfig unless asked.
                buildFeatures { buildConfig = true }
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                packaging {
                    resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
                }
            }
            configureKotlinJvmTarget()
            configureUnitTests()
            configureInstrumentedTests()
            configureNoInternetPermissionCheck()

            dependencies {
                "implementation"(libs.findLibrary("kotlinx-coroutines-core").get())
                "implementation"(libs.findLibrary("kotlinx-serialization-json").get())
            }
        }
    }
}
