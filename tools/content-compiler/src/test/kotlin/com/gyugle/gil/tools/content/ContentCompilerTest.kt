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
package com.gyugle.gil.tools.content

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.gyugle.gil.tools.content.check.Rule
import com.gyugle.gil.tools.content.database.RoomSchema
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ContentCompilerTest {

    @TempDir
    lateinit var temp: File

    private fun sample() = SampleContent(File(temp, "content"))

    @Test
    fun compile_sample_hasNoErrors() {
        val result = sample().compile(fonts = true)

        assertThat(result.errors).isEmpty()
    }

    @Test
    fun compile_sampleWithSchema_writesFileRoomCanOpen() {
        val out = File(temp, "out")

        val result = sample().compile(schema = SampleContent.schemaFile, out = out)

        assertThat(result.errors).isEmpty()
        database(File(out, ContentCompiler.DATABASE)).use { db ->
            assertThat(db.single("PRAGMA user_version")).isEqualTo("1")
            assertThat(db.single("PRAGMA journal_mode")).isEqualTo("delete")
            assertThat(db.single("SELECT count(*) FROM sqlite_master WHERE name = 'room_master_table'")).isEqualTo("0")
            assertThat(db.single("SELECT count(*) FROM lesson")).isEqualTo("3")
            assertThat(db.single("SELECT group_concat(id) FROM lesson ORDER BY coursePosition"))
                .isEqualTo("lesson.s1-01.1,lesson.a1-01.1,lesson.a1-01.2")
            assertThat(
                db.single("SELECT lessonId FROM sense WHERE id = 'sense.run.v.move-fast'")
            ).isEqualTo("lesson.a1-01.2")
            assertThat(db.single("SELECT stageId FROM sense WHERE id = 'sense.run.v.manage'")).isEqualTo("stage.b1")
            assertThat(db.single("SELECT ipa FROM word WHERE id = 'word.student'")).isEqualTo("ˈstuːdənt")
            assertThat(db.single("SELECT searchKey FROM word WHERE id = 'word.run'")).isEqualTo(" run runs ran running")
            assertThat(
                db.single("SELECT text FROM word_form WHERE wordId = 'word.run' AND kind = 'past'")
            ).isEqualTo("ran")
            assertThat(db.single("SELECT count(*) FROM lesson_item WHERE lessonId = 'lesson.a1-01.1'")).isEqualTo("5")
            assertThat(db.single("SELECT chunks FROM sentence WHERE chunks IS NOT NULL"))
                .isEqualTo(
                    """[{"start":0,"end":1,"role":"who"},{"start":2,"end":5,"role":"do"},{"start":6,"end":19,"role":"when"}]"""
                )
        }
        val manifest = Json.parseToJsonElement(File(out, ContentCompiler.MANIFEST).readText()).jsonObject
        assertThat(manifest.getValue("revision").jsonPrimitive.content).isEqualTo(result.revision!!)
        assertThat(manifest.getValue("counts").jsonObject.getValue("lessons").jsonPrimitive.int).isEqualTo(3)
    }

    @Test
    fun compile_sentence_linksEachWordToTheSenseTaughtByThen() {
        val out = File(temp, "out")

        sample().compile(schema = SampleContent.schemaFile, out = out)

        database(File(out, ContentCompiler.DATABASE)).use { db ->
            val tokens = db.single(
                "SELECT tokens FROM sentence WHERE text = 'I run every morning.' AND chunks IS NULL LIMIT 1"
            )
            assertThat(tokens).contains(""""start":2,"end":5,"word":"word.run","sense":"sense.run.v.move-fast"""")
        }
    }

    @Test
    fun compile_stepPayload_isJsonWithTypeKey() {
        val out = File(temp, "out")

        sample().compile(schema = SampleContent.schemaFile, out = out)

        database(File(out, ContentCompiler.DATABASE)).use { db ->
            val payload = db.single(
                "SELECT payload FROM lesson_step WHERE lessonId = 'lesson.a1-01.1' AND position = 2"
            )
            assertThat(payload).contains(""""type":"arrange"""")
            assertThat(payload).contains(""""tiles":["I","am","a","student"]""")
        }
    }

    @Test
    fun compile_contraction_isStoredWithTypographicApostrophe() {
        val out = File(temp, "out")
        val content = sample()
            .edit(
                "units/a1/a1-01.yaml",
                "      - type: dictation\n        en: I am a student.",
                "      - type: dictation\n        en: I'm a student."
            )

        val result = content.compile(schema = SampleContent.schemaFile, out = out)

        assertThat(result.errors).isEmpty()
        database(File(out, ContentCompiler.DATABASE)).use { db ->
            assertThat(db.single("SELECT count(*) FROM sentence WHERE text = 'I’m a student.'")).isEqualTo("1")
        }
    }

    @Test
    fun compile_twice_givesTheSameRevision() {
        val first = sample().compile(schema = SampleContent.schemaFile, out = File(temp, "a"))
        val second = SampleContent(
            File(temp, "content2")
        ).compile(schema = SampleContent.schemaFile, out = File(temp, "b"))

        assertThat(first.revision).isNotNull()
        assertThat(second.revision).isEqualTo(first.revision)
        assertThat(
            File(temp, "a/content.db").readBytes().contentEquals(File(temp, "b/content.db").readBytes())
        ).isTrue()
    }

    @Test
    fun compile_changedContent_changesRevision() {
        val first = sample().compile(schema = SampleContent.schemaFile, out = File(temp, "a"))
        val changed = SampleContent(File(temp, "content2"))
            .edit("lexicon/words.yaml", "ko: 아침", "ko: 아침, 오전")
            .compile(schema = SampleContent.schemaFile, out = File(temp, "b"))

        assertThat(changed.revision).isNotNull()
        assertThat(changed.revision == first.revision).isFalse()
    }

    @Test
    fun compile_withErrors_writesNoDatabase() {
        val out = File(temp, "out")

        sample().edit(
            "units/a1/a1-01.yaml",
            "en: I run every morning.\n        ko: 저는 매일 아침 달려요.\n      - type: readPassage",
            "en: I ran every morning.\n        ko: 저는 매일 아침 달렸어요.\n      - type: readPassage"
        )
            .compile(schema = SampleContent.schemaFile, out = out)

        assertThat(File(out, ContentCompiler.DATABASE).exists()).isFalse()
    }

    @Test
    fun compile_updateLock_writesEveryIdSortedAndKeepsRetired() {
        val content = sample().write("ids.lock", "sense.old.n.gone retired")

        content.compile(updateLock = true)

        val lines = File(content.dir, "ids.lock").readLines().filterNot { it.startsWith("#") }
        assertThat(lines).contains("sense.old.n.gone retired")
        assertThat(lines).contains("lesson.a1-01.2")
        assertThat(lines).contains("word.run")
        assertThat(lines.map { it.substringBefore(' ') }).isEqualTo(lines.map { it.substringBefore(' ') }.sorted())
        assertThat(content.compile().rules()).doesNotContain(Rule.LOCK_MISSING)
    }

    @Test
    fun compile_report_listsLessonsInCourseOrderWithTheirItems() {
        val report = File(temp, "report.md")

        sample().compile(report = report)

        val lessons = report.readLines().filter {
            it.startsWith("- lesson.")
        }.map { it.removePrefix("- ").substringBefore(' ') }
        assertThat(lessons).containsExactly("lesson.s1-01.1", "lesson.a1-01.1", "lesson.a1-01.2")
        assertThat(report.readText()).contains("- lesson.a1-01.2 매일 아침 달려요: 5 steps, 10 items known after it")
        assertThat(report.readText()).contains("  - sense.run.v.move-fast")
    }

    @Test
    fun compile_sampleWithSchema_tablesMatchTheRoomSchema() {
        val out = File(temp, "out")
        val schema = RoomSchema.parse(SampleContent.schemaFile.readText())

        sample().compile(schema = SampleContent.schemaFile, out = out)

        database(File(out, ContentCompiler.DATABASE)).use { db ->
            for (table in schema.tables) {
                val columns = db.createStatement().use { s ->
                    s.executeQuery("PRAGMA table_info(`${table.name}`)").use { r ->
                        generateSequence {
                            if (r.next()) r.getString("name") to (r.getInt("notnull") == 1) else null
                        }.toList()
                    }
                }
                assertThat(columns).isEqualTo(table.columns.map { it.name to it.notNull })
            }
        }
    }

    @Test
    fun compile_acceptedOrder_usesTheAnswerTileSpelling() {
        val out = File(temp, "out")
        sample().edit(
            "units/a1/a1-01.yaml",
            "        en: I am a student.\n      - type: fillBlank",
            "        en: I am a student.\n        accept: [A student I am.]\n      - type: fillBlank"
        ).compile(schema = SampleContent.schemaFile, out = out)

        database(File(out, ContentCompiler.DATABASE)).use { db ->
            val payload = db.single(
                "SELECT payload FROM lesson_step WHERE lessonId = 'lesson.a1-01.1' AND position = 2"
            )
            assertThat(payload).contains(""""accepted":[["a","student","I","am"]]""")
        }
    }

    private fun database(file: File): Connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")

    private fun Connection.single(sql: String): String = createStatement().use { s ->
        s.executeQuery(sql).use { r ->
            check(r.next()) { "no row for $sql" }
            r.getString(1)
        }
    }
}
