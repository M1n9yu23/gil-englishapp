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
pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Gil"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")
include(":catalog")
include(":core:domain")
include(":core:presentation")
include(":core:navigation")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:speech")
include(":feature:home:api")
include(":feature:curriculum:api")
include(":feature:curriculum:impl:domain")
include(":feature:curriculum:impl:data")
include(":feature:curriculum:impl:presentation")
include(":feature:progress:api")
include(":feature:progress:impl:domain")
include(":feature:dictionary:api")
include(":feature:stats:api")
include(":tools:content-compiler")
