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
package com.gyugle.gil.feature.review.api

import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.feature.curriculum.api.StageId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ReviewRoute : NavKey

/**
 * A review of the cards due today, the longest overdue first, at most [limit] of them. The cards
 * answered before leaving stay done, so opening it again goes on with the rest.
 */
@Serializable
@SerialName("review.session")
data class ReviewSessionRoute(val limit: Int) : ReviewRoute

/**
 * A test of stage [stageId] for a learner who already knows it. Passing it records the lessons of
 * that stage and of every stage before it as finished. Offer it only when [hasStageTest] is true.
 */
@Serializable
@SerialName("review.stage_test")
data class StageTestRoute(val stageId: StageId) : ReviewRoute

/** A [ReviewSessionRoute.limit] that takes every card due today. */
const val ALL_DUE_CARDS: Int = Int.MAX_VALUE

/**
 * The data URI that opens a review of every card due today, such as from a launcher shortcut or the
 * widget. Put it on an explicit intent to the app's launch activity.
 */
const val REVIEW_DEEP_LINK = "gil://review"
