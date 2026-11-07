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
package com.gyugle.gil.feature.review.impl.domain.model

import com.gyugle.gil.feature.progress.api.ReviewRating

/** How an answer to a card turned out, and the rating it gives the item. */
enum class AnswerGrade(val rating: ReviewRating) {
    CORRECT(ReviewRating.GOOD),

    /** One letter off in a typed answer. */
    CLOSE(ReviewRating.HARD),

    WRONG(ReviewRating.AGAIN)
}
