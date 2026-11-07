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
import com.gyugle.gil.convention.configureDocTranslationCheck
import org.gradle.api.Plugin
import org.gradle.api.Project

/** Repository-wide checks that belong to no module. Applied to the root project only. */
class RootConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            check(this == rootProject) { "gil.root is for the root project, not $path" }
            // Gives the root project a `check` task to hang repository checks on.
            pluginManager.apply("base")
            configureDocTranslationCheck()
        }
    }
}
