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

import android.app.Application
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isSameInstanceAs
import java.io.File
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ContentDatabaseHolderTest {
    private val context = RuntimeEnvironment.getApplication()

    private fun manifest() = Json.parseToJsonElement(
        context.assets.open("content/manifest.json").bufferedReader().use { it.readText() }
    ).jsonObject

    private fun revisionFile() = "content-${manifest().getValue("revision").jsonPrimitive.content}.db"

    @Test
    fun get_shippedAsset_opensWithEveryStage() = runTest {
        val database = ContentDatabaseHolder(context).get()

        val expected = manifest().getValue("counts").jsonObject.getValue("stages").jsonPrimitive.int
        assertThat(database.courseDao().stages().size).isEqualTo(expected)
        assertThat(context.getDatabasePath(revisionFile()).exists()).isEqualTo(true)
    }

    @Test
    fun get_calledTwice_returnsTheSameDatabase() = runTest {
        val holder = ContentDatabaseHolder(context)

        assertThat(holder.get()).isSameInstanceAs(holder.get())
    }

    @Test
    fun get_afterACopyFailure_triesAgainFromTheStart() = runTest {
        val blockable = BlockableDatabaseContext(context)
        val holder = ContentDatabaseHolder(blockable)

        assertFailure { holder.get() }
        blockable.blocked = false

        assertThat(holder.get().courseDao().stages()).isNotEmpty()
    }

    @Test
    fun get_filesOfAnotherRevision_deletesThemAndKeepsOtherDatabases() = runTest {
        val directory = context.getDatabasePath("x").parentFile!!.apply { mkdirs() }
        val stale =
            listOf("content-0000000000000000.db", "content-0000000000000000.db-wal", "content-1111111111111111.db-shm")
        (stale + "progress.db").forEach { File(directory, it).writeText("") }

        ContentDatabaseHolder(context).get()

        val left = directory.list()!!.filter {
            !it.endsWith("-journal") && !it.endsWith("-wal") && !it.endsWith("-shm")
        }
        assertThat(left).containsExactlyInAnyOrder(revisionFile(), "progress.db")
        stale.forEach { assertThat(File(directory, it).exists()).isEqualTo(false) }
    }
}
