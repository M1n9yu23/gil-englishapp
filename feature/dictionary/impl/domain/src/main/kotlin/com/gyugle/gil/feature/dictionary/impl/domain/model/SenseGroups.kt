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
package com.gyugle.gil.feature.dictionary.impl.domain.model

import com.gyugle.gil.feature.curriculum.api.WordSense

/**
 * A word's senses split for its page, each list in the content's order. [learnedAny] is false
 * when the learner has met none of them and [now] holds the ones the course teaches first.
 */
data class SenseGroups(val now: List<WordSense>, val later: List<WordSense>, val learnedAny: Boolean)
