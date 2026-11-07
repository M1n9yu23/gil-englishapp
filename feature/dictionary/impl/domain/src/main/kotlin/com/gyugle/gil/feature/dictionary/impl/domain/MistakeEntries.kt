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
package com.gyugle.gil.feature.dictionary.impl.domain

import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.dictionary.impl.domain.model.MistakeEntry
import com.gyugle.gil.feature.progress.api.Mistake

/**
 * Pairs each mistake with its item's line, keeping the order of [mistakes]. A mistake on an item
 * the content no longer has is left out.
 */
fun mistakeEntries(mistakes: List<Mistake>, items: List<LessonItem>): List<MistakeEntry> {
    val byId = items.associateBy { it.id }
    return mistakes.mapNotNull { mistake -> byId[mistake.itemId]?.let { MistakeEntry(it, mistake.count) } }
}
