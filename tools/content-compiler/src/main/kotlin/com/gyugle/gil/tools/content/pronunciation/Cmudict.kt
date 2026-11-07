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
package com.gyugle.gil.tools.content.pronunciation

/**
 * The CMU Pronouncing Dictionary: lowercase word to its ARPAbet pronunciations, first one first.
 *
 * Lines look like `read(2) R IY1 D # comment`. The bundled copy is `cmudict/cmudict.dict`.
 */
class Cmudict(private val entries: Map<String, List<List<String>>>) {

    fun pronunciations(word: String): List<List<String>> = entries[word.lowercase()].orEmpty()

    companion object {
        fun parse(lines: Sequence<String>): Cmudict {
            val entries = linkedMapOf<String, MutableList<List<String>>>()
            for (line in lines) {
                val content = line.substringBefore('#').trim()
                if (content.isEmpty()) continue
                val parts = content.split(' ').filter { it.isNotEmpty() }
                val word = parts.first().substringBefore('(').lowercase()
                entries.getOrPut(word) { mutableListOf() } += parts.drop(1)
            }
            return Cmudict(entries)
        }

        /** Loads the copy bundled with the compiler. */
        fun bundled(): Cmudict {
            val stream = checkNotNull(Cmudict::class.java.getResourceAsStream("/cmudict/cmudict.dict")) {
                "cmudict.dict is missing from the compiler resources"
            }
            return stream.bufferedReader().useLines { parse(it) }
        }
    }
}
