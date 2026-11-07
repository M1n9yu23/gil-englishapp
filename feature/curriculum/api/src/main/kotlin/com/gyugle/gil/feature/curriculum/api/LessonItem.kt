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

import com.gyugle.gil.feature.curriculum.api.StudyItemId

/**
 * A study item a lesson introduces, as the lesson's summary lists it.
 *
 * [text] is English: the word of a sense, the text of a pattern, or the letters that spell a sound.
 * [ipa] has no slashes. A sound has no [meaningKo].
 */
data class LessonItem(val id: StudyItemId, val text: String, val ipa: String?, val meaningKo: String?)
