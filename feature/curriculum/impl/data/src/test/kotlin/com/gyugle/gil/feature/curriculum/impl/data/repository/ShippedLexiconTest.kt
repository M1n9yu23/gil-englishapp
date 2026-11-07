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
package com.gyugle.gil.feature.curriculum.impl.data.repository

import android.app.Application
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.EntryId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.impl.data.ContentDatabaseHolder
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Reads every entry and sentence of the content that ships, so a value the app cannot map, such as
 * an unknown part of speech or chunk role, fails here.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class ShippedLexiconTest {
    private val holder = ContentDatabaseHolder(RuntimeEnvironment.getApplication())
    private val repository = RoomLexiconRepository(holder)

    private suspend fun column(sql: String): List<String> {
        val cursor = holder.get().openHelper.readableDatabase.query(sql)
        return cursor.use { generateSequence { if (it.moveToNext()) it.getString(0) else null }.toList() }
    }

    @Test
    fun getEntry_everyShippedEntry_reads() = runTest {
        val ids = column("SELECT id FROM word UNION ALL SELECT id FROM pattern UNION ALL SELECT id FROM sound")

        assertThat(ids.size).isGreaterThan(0)
        ids.forEach { id ->
            val entry = repository.getEntry(checkNotNull(EntryId.parse(id)) { "No page for $id" })
            assertThat(entry, name = id).isInstanceOf(Outcome.Success::class)
        }
    }

    @Test
    fun getEntry_everyShippedWord_hasASenseWithAnExample() = runTest {
        column("SELECT id FROM word").forEach { id ->
            val word = (repository.getEntry(checkNotNull(EntryId.parse(id))) as Outcome.Success).data as WordEntry
            assertThat(word.senses.isNotEmpty() && word.senses.all { it.examples.isNotEmpty() }, name = id)
                .isEqualTo(true)
        }
    }

    @Test
    fun getSentence_everyShippedSentence_decodes() = runTest {
        val ids = column("SELECT id FROM sentence").map { it.toLong() }

        assertThat(ids.size).isGreaterThan(0)
        ids.forEach { id ->
            val breakdown = repository.getSentence(id)
            assertThat(breakdown, name = "sentence $id").isInstanceOf(Outcome.Success::class)
            val text = ((breakdown as Outcome.Success).data).sentence.text
            breakdown.data.words.forEach { word ->
                // Offsets point inside the text, so the screen can mark the word.
                assertThat(word.end <= text.length && word.start < word.end, name = "sentence $id").isEqualTo(true)
            }
        }
    }

    @Test
    fun search_shippedWords_findThemselves() = runTest {
        column("SELECT text FROM word").forEach { text ->
            val hits = (repository.search(text, limit = 5) as Outcome.Success).data
            assertThat(hits.firstOrNull()?.text, name = text).isEqualTo(text)
        }
    }
}
