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

/** A stage test asks about this many of the stage's items, or about all of them when it has fewer. */
const val STAGE_TEST_QUESTIONS: Int = 20

/** Below this many items a stage has no test: a handful of answers shows too little. */
const val STAGE_TEST_MIN_ITEMS: Int = 8

/** Whether a stage whose lessons introduce [itemCount] items can be skipped by a test. */
fun hasStageTest(itemCount: Int): Boolean = itemCount >= STAGE_TEST_MIN_ITEMS

/** How many questions the test of a stage with [itemCount] items asks. */
fun stageTestQuestions(itemCount: Int): Int = minOf(itemCount, STAGE_TEST_QUESTIONS)
