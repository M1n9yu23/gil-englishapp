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

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.w3c.dom.Element

private const val INTERNET = "android.permission.INTERNET"
private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

/**
 * Fails when a variant's merged manifest asks for INTERNET. The app works offline by design, so a
 * library that adds the permission must be caught at build time, not noticed on the Play listing.
 */
@CacheableTask
abstract class VerifyNoInternetPermissionTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:Input
    abstract val variantName: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun verify() {
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val document = factory.newDocumentBuilder().parse(mergedManifest.get().asFile)
        val requested = listOf("uses-permission", "uses-permission-sdk-23").any { tag ->
            val nodes = document.getElementsByTagName(tag)
            (0 until nodes.length).any { index ->
                (nodes.item(index) as Element).getAttributeNS(ANDROID_NS, "name") == INTERNET
            }
        }
        val variant = variantName.get()
        if (requested) {
            throw GradleException(
                "The merged manifest of '$variant' requests $INTERNET. Find what adds it in " +
                    "build/outputs/logs/manifest-merger-$variant-report.txt and remove the dependency " +
                    "or drop the permission with tools:node=\"remove\"."
            )
        }
        report.get().asFile.writeText("$variant: no $INTERNET\n")
    }
}

/** Registers `verify<Variant>NoInternetPermission` for every variant and hangs them all on `check`. */
internal fun Project.configureNoInternetPermissionCheck() {
    val verifyAll = tasks.register("verifyNoInternetPermission") {
        group = "verification"
        description = "Fails if any variant's merged manifest requests $INTERNET."
    }
    tasks.named("check") { dependsOn(verifyAll) }

    extensions.configure<ApplicationAndroidComponentsExtension> {
        onVariants { variant ->
            val name = variant.name
            val verify = tasks.register<VerifyNoInternetPermissionTask>(
                "verify${name.replaceFirstChar(Char::uppercaseChar)}NoInternetPermission"
            ) {
                group = "verification"
                description = "Fails if the merged manifest of $name requests $INTERNET."
                mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
                variantName.set(name)
                report.set(layout.buildDirectory.file("reports/no-internet-permission/$name.txt"))
            }
            verifyAll.configure { dependsOn(verify) }
        }
    }
}
