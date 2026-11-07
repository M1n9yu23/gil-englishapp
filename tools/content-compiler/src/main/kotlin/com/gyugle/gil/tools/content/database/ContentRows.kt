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

/** Rows to insert, by table, in insertion order. Values are String, Int, Long or null. */
class ContentRows {
    private val tables = linkedMapOf<String, MutableList<Map<String, Any?>>>()

    val all: Map<String, List<Map<String, Any?>>> get() = tables

    fun add(table: String, vararg values: Pair<String, Any?>) {
        tables.getOrPut(table) { mutableListOf() } += linkedMapOf(*values)
    }

    fun count(table: String): Int = tables[table]?.size ?: 0
}
