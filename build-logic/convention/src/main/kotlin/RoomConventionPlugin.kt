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
import androidx.room.gradle.RoomExtension
import com.gyugle.gil.convention.applyKsp
import com.gyugle.gil.convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Applied by the module that owns the entities (`feature:<name>:impl:data`), never `core:database`. */
class RoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("androidx.room")
            applyKsp()

            extensions.configure<RoomExtension> {
                // Committed: migration tests and auto-migrations need every previous schema.
                schemaDirectory("$projectDir/schemas")
            }
            dependencies {
                "implementation"(libs.findLibrary("androidx-room-runtime").get())
                "ksp"(libs.findLibrary("androidx-room-compiler").get())
            }
        }
    }
}
