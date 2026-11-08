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
package com.gyugle.gil.feature.settings.impl.presentation.licenses

import android.content.Context
import androidx.compose.runtime.Immutable
import com.mikepenz.aboutlibraries.Libs
import com.mikepenz.aboutlibraries.util.withContext
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** One entry of the license list, with every license that applies to it. */
@Immutable
data class LibraryUi(
    val id: String,
    val name: String,
    val version: String?,
    val developers: String?,
    val licenses: List<LicenseUi>
)

@Immutable
data class LicenseUi(val name: String, val text: String?)

/**
 * The libraries, typefaces and data in the app, read from `aboutlibraries.json`, which the app
 * module generates at build time. Read once and kept: the list never changes while the app runs.
 */
@Singleton
class OpenSourceLicenses @Inject constructor(@param:ApplicationContext private val context: Context) {
    private val mutex = Mutex()
    private var cached: List<LibraryUi>? = null

    /** Empty when the generated list is missing, which only a broken build causes. */
    suspend fun load(): List<LibraryUi> = mutex.withLock {
        cached ?: withContext(Dispatchers.IO) { read() }.also { cached = it }
    }

    private fun read(): List<LibraryUi> = Libs.Builder().withContext(context).build().libraries
        .map { library ->
            LibraryUi(
                id = library.uniqueId,
                name = library.name,
                version = library.artifactVersion,
                developers = library.developers.mapNotNull { it.name }.distinct().joinToString().ifBlank { null }
                    ?: library.organization?.name,
                licenses = library.licenses.map { LicenseUi(it.name, it.licenseContent?.let(::reflow)) }
            )
        }
        .sortedBy { it.name.lowercase() }
}

/**
 * License files are wrapped at about 80 columns for a terminal; on a phone that leaves ragged half
 * lines. Lines within a paragraph are joined so the text wraps to the screen; blank lines still
 * separate paragraphs.
 */
internal fun reflow(text: String): String = text.replace("\r\n", "\n")
    .split(Regex("\n[ \t]*\n"))
    .map { paragraph -> paragraph.lines().joinToString(" ") { it.trim() }.trim() }
    .filter { it.isNotEmpty() }
    .joinToString("\n\n")
