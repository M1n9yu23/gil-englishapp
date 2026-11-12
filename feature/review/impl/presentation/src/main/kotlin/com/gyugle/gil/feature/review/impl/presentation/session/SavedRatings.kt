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
package com.gyugle.gil.feature.review.impl.presentation.session

import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.progress.api.ReviewRating

private const val SEPARATOR = '|'

/** Ratings in a form a SavedStateHandle keeps, read back by [toRatings]. */
internal fun List<Pair<StudyItemId, ReviewRating>>.toSavedRatings(): ArrayList<String> =
    ArrayList(map { (id, rating) -> "${rating.name}$SEPARATOR${id.value}" })

/** Ratings saved by [toSavedRatings]. An entry that cannot be read is left out. */
internal fun List<String>.toRatings(): List<Pair<StudyItemId, ReviewRating>> = mapNotNull { stored ->
    val rating = ReviewRating.entries.firstOrNull { it.name == stored.substringBefore(SEPARATOR) }
    val id = StudyItemId.parse(stored.substringAfter(SEPARATOR))
    if (rating == null || id == null) null else id to rating
}
