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

import kotlinx.serialization.Serializable

/**
 * Something the learner reviews on a schedule: a word sense, a pattern or a sound.
 *
 * The content ID prefix tells the kinds apart, so a stored ID needs no separate kind column. Each
 * kind checks its prefix when created, so an ID can never be read back as another kind.
 */
sealed interface StudyItemId {
    val value: String

    companion object {
        /** Returns null for an ID that is not a study item, such as `word.run` or `lesson.s1-01.1`. */
        fun parse(value: String): StudyItemId? = when {
            value.hasName(SenseId.PREFIX) -> SenseId(value)
            value.hasName(PatternId.PREFIX) -> PatternId(value)
            value.hasName(SoundId.PREFIX) -> SoundId(value)
            else -> null
        }
    }
}

/** One meaning of a word, such as `sense.run.v.move-fast`. */
@JvmInline
@Serializable
value class SenseId(override val value: String) : StudyItemId {
    init {
        require(value.hasName(PREFIX)) { "Not a sense ID: $value" }
    }

    internal companion object {
        const val PREFIX = "sense."
    }
}

/** A phrase pattern, such as `pattern.how-are-you`. */
@JvmInline
@Serializable
value class PatternId(override val value: String) : StudyItemId {
    init {
        require(value.hasName(PREFIX)) { "Not a pattern ID: $value" }
    }

    internal companion object {
        const val PREFIX = "pattern."
    }
}

/** A letter name, sound or spelling pattern, such as `sound.letter-a`. */
@JvmInline
@Serializable
value class SoundId(override val value: String) : StudyItemId {
    init {
        require(value.hasName(PREFIX)) { "Not a sound ID: $value" }
    }

    internal companion object {
        const val PREFIX = "sound."
    }
}

private fun String.hasName(prefix: String) = length > prefix.length && startsWith(prefix)
