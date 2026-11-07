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
package com.gyugle.gil.feature.dictionary.impl.presentation.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.dictionary.impl.presentation.R

/** Content text in both app languages, such as a stage title; the screen shows the app's one. */
@Immutable
data class LocalizedTextUi(val ko: String, val en: String) {
    @Composable
    fun asString(): String = if (stringResource(R.string.dictionary_content_language) == "ko") ko else en
}

internal fun LocalizedText.toLocalizedTextUi(): LocalizedTextUi = LocalizedTextUi(ko = ko, en = en)
