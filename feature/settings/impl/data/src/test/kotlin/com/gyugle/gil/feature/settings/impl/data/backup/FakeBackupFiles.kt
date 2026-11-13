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
package com.gyugle.gil.feature.settings.impl.data.backup

/** Documents by URI. A URI that is not in [files] cannot be read; [writable] false fails every write. */
internal class FakeBackupDocuments : BackupDocuments {
    val files = mutableMapOf<String, String>()
    var writable = true

    override suspend fun read(uri: String): String? = files[uri]

    override suspend fun write(uri: String, text: String): Boolean {
        if (writable) files[uri] = text
        return writable
    }
}

internal class FakeBackupSnapshots : BackupSnapshots {
    val saved = mutableListOf<String>()
    var writable = true

    override suspend fun save(text: String): Boolean {
        if (writable) saved += text
        return writable
    }

    override suspend fun delete() = saved.clear()
}
