package com.gyugle.gil.convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

internal const val ROOT_PACKAGE = "com.gyugle.gil"

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Project configuration lives in the catalog so no module hardcodes an SDK level. */
internal fun Project.catalogVersion(alias: String): String = libs.findVersion(alias).get().requiredVersion

internal fun Project.catalogVersionInt(alias: String): Int = catalogVersion(alias).toInt()

/** `:feature:user-profile:impl:data` -> `com.gyugle.gil.feature.user_profile.impl.data`. */
internal fun Project.pathToPackageName(): String =
    ROOT_PACKAGE + path.replace(":", ".").replace("-", "_").lowercase()
