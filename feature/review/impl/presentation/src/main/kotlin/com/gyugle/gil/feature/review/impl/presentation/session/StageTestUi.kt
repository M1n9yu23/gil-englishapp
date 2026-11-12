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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.review.impl.presentation.R

/**
 * Content text in both app languages, picked in composition by the language the app's resources
 * resolved to, so it always matches the rest of the screen.
 */
@Immutable
data class LocalizedTextUi(val ko: String, val en: String) {
    @Composable
    fun asString(): String = if (stringResource(R.string.review_content_language) == "ko") ko else en
}

internal fun LocalizedText.toLocalizedTextUi(): LocalizedTextUi = LocalizedTextUi(ko = ko, en = en)

/**
 * A stage test about to start: [questions] questions about [stage], of which [passMark] must be
 * right. Passing records [lessons] lessons as finished, the ones left in the stage and before it.
 */
@Immutable
data class TestIntroUi(val stage: LocalizedTextUi, val questions: Int, val passMark: Int, val lessons: Int)

/**
 * How a stage test went. [right] of [answered] questions were right; a failed test can end before
 * all [questions] are asked. A pass recorded [lessons] lessons as finished. [missed] are the items
 * answered wrong.
 */
@Immutable
data class TestResultUi(
    val stage: LocalizedTextUi,
    val passed: Boolean,
    val right: Int,
    val answered: Int,
    val questions: Int,
    val lessons: Int,
    val missed: List<ResultItemUi>
) {
    val endedEarly: Boolean get() = answered < questions
}
