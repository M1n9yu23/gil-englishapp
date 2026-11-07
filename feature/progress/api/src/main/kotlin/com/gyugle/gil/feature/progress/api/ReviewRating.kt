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
package com.gyugle.gil.feature.progress.api

/**
 * How well the learner recalled an item, decided by the app from the answer, not chosen by the
 * learner.
 *
 * There is no "easy": a quick right answer is still [GOOD]. Rating it easy would stretch the next
 * interval past what one fast answer shows.
 */
enum class ReviewRating {
    /** Wrong. */
    AGAIN,

    /** Right on the second try, or with one letter off. */
    HARD,

    /** Right on the first try. */
    GOOD
}
