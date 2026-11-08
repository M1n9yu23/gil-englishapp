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
package com.gyugle.gil.feature.listening.impl.domain.model

import com.gyugle.gil.feature.curriculum.api.DialogueLine
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.StudyItemId

/** One thing a playlist plays, before it becomes a [Track]. */
sealed interface PlaylistEntry {
    /** A word sense or a pattern: its English, its Korean meaning and the dictionary's examples. */
    data class Item(val id: StudyItemId, val english: String, val korean: String, val examples: List<Sentence>) :
        PlaylistEntry

    /** The [index]th dialogue of a lesson. */
    data class Dialogue(val lessonId: LessonId, val index: Int, val lines: List<DialogueLine>) : PlaylistEntry
}
