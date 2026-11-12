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
package com.gyugle.gil.convention

import com.google.devtools.ksp.gradle.KspAATask
import org.gradle.api.Project
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.kotlin.dsl.registerIfAbsent
import org.gradle.kotlin.dsl.withType

/** Held by a KSP task while it runs, so that only one runs at a time in the build. */
abstract class KspLock : BuildService<BuildServiceParameters.None>

/**
 * Applies KSP and lets its tasks run one at a time. KSP tasks running side by side in one Gradle
 * daemon sometimes failed to resolve classpath types such as Context. Often the task still passed
 * with nothing generated, so a ViewModel or binding was missing only when the app ran.
 */
internal fun Project.applyKsp() {
    pluginManager.apply("com.google.devtools.ksp")
    val lock = gradle.sharedServices.registerIfAbsent("kspLock", KspLock::class) {
        maxParallelUsages.set(1)
    }
    tasks.withType<KspAATask>().configureEach { usesService(lock) }
}
