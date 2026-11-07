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

import kotlinx.serialization.Serializable

/** Text shown in the app chrome, where the learner may have chosen either app language. */
@Serializable
data class Localized(val ko: String, val en: String)

/** `content/course.yaml`: the stages in order and the units of each stage in order. */
@Serializable
data class CourseSource(
    val stages: List<StageSource>,
    /**
     * Inflection kind to the study item that teaches it. Before that item is introduced, a sentence
     * may not use that kind of form (for example `past: pattern.past-simple`).
     */
    val inflections: Map<String, String> = emptyMap()
)

@Serializable
data class StageSource(
    val id: String,
    val title: Localized,
    val canDo: List<Localized> = emptyList(),
    val units: List<String> = emptyList()
)

/** One file under `content/units/<stage>/`. */
@Serializable
data class UnitSource(val id: String, val title: Localized, val lessons: List<LessonSource>)

@Serializable
data class LessonSource(
    val id: String,
    val title: Localized,
    val goal: Localized,
    /** Sense, pattern and sound IDs this lesson teaches for the first time. */
    val introduces: List<String> = emptyList(),
    val steps: List<StepSource>
)

/** An English sentence with its Korean translation. */
@Serializable
data class ExampleSource(
    val en: String,
    val ko: String,
    /** Word-by-word Korean gloss for the sentence breakdown sheet. */
    val literal: String? = null,
    val grammar: String? = null
)

/** An answer choice. Exactly one of [en] and [ko] is set; every wrong choice says [why]. */
@Serializable
data class OptionSource(
    val en: String? = null,
    val ko: String? = null,
    val correct: Boolean = false,
    val why: String? = null
)
