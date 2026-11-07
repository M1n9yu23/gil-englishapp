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
package com.gyugle.gil.tools.content.source

import com.charleskorn.kaml.PolymorphismStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlException
import com.gyugle.gil.tools.content.check.Issues
import com.gyugle.gil.tools.content.check.Rule
import java.io.File
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer

/** A parsed value and the content-relative path of the file it came from. */
data class Located<T>(val file: String, val value: T)

/** IDs from `content/ids.lock`. [retired] IDs were removed on purpose and may never come back. */
data class IdsLock(val locked: Set<String>, val retired: Set<String>)

class LoadedContent(
    val course: Located<CourseSource>?,
    val units: List<Located<UnitSource>>,
    val words: List<Located<WordSource>>,
    val patterns: List<Located<PatternSource>>,
    val sounds: List<Located<SoundSource>>,
    val grammar: List<Located<GrammarSource>>,
    val lock: IdsLock
)

/** Reads every file of the `content/` directory. Parse failures become [Rule.YAML_INVALID] issues. */
class ContentLoader(private val root: File, private val issues: Issues) {

    private val yaml = Yaml(
        configuration = YamlConfiguration(
            strictMode = true,
            polymorphismStyle = PolymorphismStyle.Property,
            polymorphismPropertyName = "type"
        )
    )

    fun load(): LoadedContent {
        root.walkTopDown().filter { it.isFile && it.extension == "yml" }.forEach {
            issues.add(Rule.YAML_INVALID, relative(it), "content files end in .yaml; this one is skipped")
        }
        val courseFile = File(root, COURSE)
        val course = if (courseFile.isFile) {
            decode(courseFile, CourseSource.serializer())
        } else {
            issues.add(Rule.YAML_INVALID, COURSE, "the file is missing")
            null
        }
        return LoadedContent(
            course = course,
            units = yamlFiles("units").mapNotNull { decode(it, UnitSource.serializer()) },
            words = decodeLists("lexicon", WordSource.serializer()),
            patterns = decodeList(File(root, "patterns.yaml"), PatternSource.serializer()),
            sounds = decodeList(File(root, "sounds.yaml"), SoundSource.serializer()),
            grammar = decodeLists("grammar", GrammarSource.serializer()),
            lock = readLock()
        )
    }

    private fun <T> decodeLists(dir: String, item: KSerializer<T>): List<Located<T>> =
        yamlFiles(dir).flatMap { decodeList(it, item) }

    private fun <T> decodeList(file: File, item: KSerializer<T>): List<Located<T>> {
        if (!file.isFile) return emptyList()
        val located = decode(file, ListSerializer(item)) ?: return emptyList()
        return located.value.map { Located(located.file, it) }
    }

    private fun <T> decode(file: File, strategy: DeserializationStrategy<T>): Located<T>? {
        val path = relative(file)
        return try {
            Located(path, yaml.decodeFromString(strategy, file.readText()))
        } catch (e: YamlException) {
            issues.add(Rule.YAML_INVALID, "$path:${e.line}:${e.column}", e.message.orEmpty())
            null
        } catch (e: SerializationException) {
            issues.add(Rule.YAML_INVALID, path, e.message.orEmpty())
            null
        }
    }

    private fun yamlFiles(dir: String): List<File> {
        val base = File(root, dir)
        if (!base.isDirectory) return emptyList()
        val walk = base.walkTopDown()
        return walk.filter { it.isFile && it.extension == "yaml" }.sortedBy { relative(it) }.toList()
    }

    private fun readLock(): IdsLock {
        val file = File(root, LOCK)
        if (!file.isFile) return IdsLock(emptySet(), emptySet())
        val locked = mutableSetOf<String>()
        val retired = mutableSetOf<String>()
        file.readLines().forEachIndexed { index, raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) return@forEachIndexed
            val parts = line.split(Regex("\\s+"))
            when {
                parts.size == 1 -> locked += parts[0]

                parts.size == 2 && parts[1] == RETIRED -> {
                    locked += parts[0]
                    retired += parts[0]
                }

                else -> issues.add(
                    Rule.ID_FORMAT,
                    "$LOCK:${index + 1}",
                    "a line is an ID, optionally followed by '$RETIRED'"
                )
            }
        }
        return IdsLock(locked, retired)
    }

    private fun relative(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    companion object {
        const val COURSE = "course.yaml"
        const val LOCK = "ids.lock"
        const val RETIRED = "retired"
    }
}
