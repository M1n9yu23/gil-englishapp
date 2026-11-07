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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The parts of a Room-exported schema JSON the writer needs. */
data class RoomSchema(val version: Int, val identityHash: String, val tables: List<Table>) {

    data class Table(val name: String, val createSql: String, val columns: List<Column>, val indexSql: List<String>)

    /** [integer] is true for INTEGER affinity; every other column here holds TEXT. */
    data class Column(val name: String, val notNull: Boolean, val hasDefault: Boolean, val integer: Boolean)

    companion object {
        /** Room writes `${TABLE_NAME}` in place of the table name in every statement. */
        private const val TABLE_NAME = "\${TABLE_NAME}"

        fun parse(json: String): RoomSchema {
            val database = Json.parseToJsonElement(json).jsonObject.getValue("database").jsonObject
            val tables = database.getValue("entities").jsonArray.map { element ->
                val entity = element.jsonObject
                val name = entity.string("tableName")
                Table(
                    name = name,
                    createSql = entity.string("createSql").replace(TABLE_NAME, name),
                    columns = entity.getValue("fields").jsonArray.map { field ->
                        val f = field.jsonObject
                        Column(
                            f.string("columnName"),
                            f["notNull"]?.jsonPrimitive?.boolean == true,
                            "defaultValue" in f,
                            f.string("affinity") == "INTEGER"
                        )
                    },
                    indexSql = entity["indices"]?.jsonArray.orEmpty().map {
                        it.jsonObject.string("createSql").replace(TABLE_NAME, name)
                    }
                )
            }
            return RoomSchema(database.getValue("version").jsonPrimitive.int, database.string("identityHash"), tables)
        }

        private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
    }
}
