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
package com.gyugle.gil.feature.curriculum.api

/** What a chunk does in its sentence: who does it, the action, to what, where and when. */
enum class ChunkRole(private val key: String) {
    WHO("who"),
    DO("do"),
    WHAT("what"),
    WHERE("where"),
    WHEN("when");

    companion object {
        /** The role the content names [key], such as `who`, or null for an unknown one. */
        fun fromKey(key: String): ChunkRole? = entries.firstOrNull { it.key == key }
    }
}
