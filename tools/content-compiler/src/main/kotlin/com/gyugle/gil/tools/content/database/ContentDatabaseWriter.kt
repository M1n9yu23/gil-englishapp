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
package com.gyugle.gil.tools.content.database

import com.gyugle.gil.tools.content.check.Issues
import com.gyugle.gil.tools.content.check.Rule
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.sql.DriverManager
import java.sql.Types

/**
 * Writes the rows into a SQLite file built from the Room schema.
 *
 * The file has no `room_master_table`, so the first time Room opens it, Room validates every table
 * against its entities and then records the identity hash. `user_version` is the database version,
 * and the journal mode is DELETE so the file is complete without `-wal` and `-shm` files. Only SQL
 * that SQLite 3.18 (API 26) understands is used.
 */
class ContentDatabaseWriter(private val schema: RoomSchema, private val issues: Issues) {

    /** Returns false and reports [Rule.SCHEMA_MISMATCH] when the rows do not fit the schema. */
    fun write(rows: ContentRows, target: File): Boolean {
        if (!fits(rows)) return false
        target.parentFile.mkdirs()
        val temporary = File(target.parentFile, target.name + ".tmp").apply { delete() }
        DriverManager.getConnection("jdbc:sqlite:${temporary.absolutePath}").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA journal_mode=DELETE") }
            connection.autoCommit = false
            connection.createStatement().use { statement ->
                schema.tables.forEach { table ->
                    statement.execute(table.createSql)
                    table.indexSql.forEach(statement::execute)
                }
            }
            for (table in schema.tables) {
                val tableRows = rows.all[table.name].orEmpty()
                if (tableRows.isEmpty()) continue
                val columns = tableRows.first().keys.toList()
                val sql = "INSERT INTO `${table.name}` (${columns.joinToString {
                    "`$it`"
                }}) VALUES (${columns.joinToString { "?" }})"
                connection.prepareStatement(sql).use { insert ->
                    for (row in tableRows) {
                        columns.forEachIndexed { index, column ->
                            when (val value = row[column]) {
                                null -> insert.setNull(index + 1, Types.NULL)
                                is Int -> insert.setLong(index + 1, value.toLong())
                                is Long -> insert.setLong(index + 1, value)
                                is String -> insert.setString(index + 1, value)
                                else -> error("unsupported value type ${value::class} in ${table.name}.$column")
                            }
                        }
                        insert.addBatch()
                    }
                    insert.executeBatch()
                }
            }
            connection.commit()
            connection.autoCommit = true
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA user_version = ${schema.version}")
                statement.execute("VACUUM")
            }
        }
        Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return true
    }

    /**
     * A short hash of the schema identity and every row. The app names its copy of the database
     * after it, so a device holding different content or an older schema copies the asset again.
     */
    fun revision(rows: ContentRows): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun feed(value: String) = digest.update((value + "\u0000").toByteArray(Charsets.UTF_8))
        feed(schema.identityHash)
        for (table in schema.tables.sortedBy { it.name }) {
            feed(table.name)
            for (row in rows.all[table.name].orEmpty()) {
                row.forEach { (column, value) -> feed("$column=${value ?: "\u0001"}") }
            }
        }
        return digest.digest().take(REVISION_BYTES).joinToString("") { "%02x".format(it) }
    }

    private fun fits(rows: ContentRows): Boolean {
        val before = issues.all.size
        val tables = schema.tables.associateBy { it.name }
        for ((name, tableRows) in rows.all) {
            val table = tables[name]
            if (table == null) {
                issues.add(
                    Rule.SCHEMA_MISMATCH,
                    "schema",
                    "the compiler writes table '$name', which the schema does not have"
                )
                continue
            }
            val known = table.columns.map { it.name }.toSet()
            tableRows.flatMap { it.keys }.toSet().filter { it !in known }.forEach {
                issues.add(
                    Rule.SCHEMA_MISMATCH,
                    "schema",
                    "the compiler writes column $name.$it, which the schema does not have"
                )
            }
            if (tableRows.map { it.keys }.distinct().size > 1) {
                issues.add(Rule.SCHEMA_MISMATCH, "schema", "rows of $name do not all have the same columns")
            }
            table.columns.forEach { column ->
                val wrong = tableRows.mapNotNull { it[column.name] }.firstOrNull { value ->
                    if (column.integer) value !is Int && value !is Long else value !is String
                }
                if (wrong != null) {
                    issues.add(
                        Rule.SCHEMA_MISMATCH,
                        "schema",
                        "column $name.${column.name} gets a ${wrong::class.simpleName}"
                    )
                }
            }
            table.columns.filter { it.notNull && !it.hasDefault }.forEach { column ->
                if (tableRows.any { it[column.name] == null }) {
                    issues.add(
                        Rule.SCHEMA_MISMATCH,
                        "schema",
                        "column $name.${column.name} is NOT NULL but the compiler leaves it empty"
                    )
                }
            }
        }
        return issues.all.size == before
    }

    private companion object {
        const val REVISION_BYTES = 8
    }
}
