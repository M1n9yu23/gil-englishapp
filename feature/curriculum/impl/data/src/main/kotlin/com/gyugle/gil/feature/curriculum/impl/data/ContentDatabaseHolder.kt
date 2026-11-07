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
package com.gyugle.gil.feature.curriculum.impl.data

import android.content.Context
import androidx.room.Room
import com.gyugle.gil.core.database.applyProjectDefaults
import com.gyugle.gil.feature.curriculum.impl.data.dto.ContentManifestDto
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

private const val ASSET_DIRECTORY = "content"
private const val FILE_PREFIX = "content-"
private val Revision = Regex("[0-9a-f]{16}")
private val ManifestJson = Json { ignoreUnknownKeys = true }

/**
 * Opens the content database that ships in the assets, once, off the main thread.
 *
 * Each content revision gets its own file, `content-<revision>.db`, so an app update with new
 * content copies the new asset instead of opening the old copy. Files of other revisions are
 * deleted first. When opening fails, the next call tries again from the start.
 */
@Singleton
class ContentDatabaseHolder @Inject constructor(@param:ApplicationContext private val context: Context) :
    ContentDatabaseProvider {
    private val mutex = Mutex()
    private var database: ContentDatabase? = null

    override suspend fun get(): ContentDatabase = mutex.withLock {
        // Kept inside the IO block: a caller cancelled while opening must not drop an open database.
        database ?: withContext(Dispatchers.IO) { open().also { database = it } }
    }

    private fun open(): ContentDatabase {
        val name = "$FILE_PREFIX${readRevision()}.db"
        deleteOtherRevisions(keep = name)
        val database = Room.databaseBuilder(context, ContentDatabase::class.java, name)
            .createFromAsset("$ASSET_DIRECTORY/content.db")
            .fallbackToDestructiveMigration(dropAllTables = true)
            .applyProjectDefaults()
            .build()
        try {
            // Room copies and checks the file on first open; doing it here surfaces a failure now.
            database.openHelper.writableDatabase
        } catch (e: Exception) {
            database.close()
            throw e
        }
        return database
    }

    private fun readRevision(): String {
        val text = context.assets.open("$ASSET_DIRECTORY/manifest.json").bufferedReader().use { it.readText() }
        val revision = ManifestJson.decodeFromString<ContentManifestDto>(text).revision
        check(Revision.matches(revision)) { "Unexpected content revision '$revision'" }
        return revision
    }

    private fun deleteOtherRevisions(keep: String) {
        context.databaseList()
            .filter { it.startsWith(FILE_PREFIX) }
            .map { it.removeSuffix("-wal").removeSuffix("-shm").removeSuffix("-journal") }
            .distinct()
            .filter { it != keep }
            // Removes the file with its -wal, -shm and -journal companions.
            .forEach { context.deleteDatabase(it) }
    }
}
