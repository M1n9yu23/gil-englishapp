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
package com.gyugle.gil.feature.lesson.impl.presentation.player

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.intl.LocaleList
import com.gyugle.gil.core.designsystem.component.appendEnglish

private val KoreanLocale = LocaleList("ko-KR")

/**
 * Korean content text, where English is wrapped in backticks: "`Hello.`는 인사예요". The English
 * parts are set and read as English.
 *
 * The text is marked Korean even when the app runs in English, so lines still break between
 * phrases rather than in the middle of a word.
 */
@Composable
internal fun KoreanProse(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val annotated = remember(text) { proseOf(text) }
    Text(text = annotated, modifier = modifier, style = style.copy(localeList = KoreanLocale), color = color)
}

/**
 * Odd-numbered parts between backticks are English. An unmatched backtick is kept as text.
 *
 * Lines may break where the English locale starts or ends, which would leave the particle of
 * "`Hi.`도" alone at the start of a line. A word joiner keeps English together with Korean that
 * touches it; screen readers skip it.
 */
internal fun proseOf(text: String): AnnotatedString = buildAnnotatedString {
    val parts = text.split('`')
    if (parts.size % 2 == 0) {
        append(text)
        return@buildAnnotatedString
    }
    parts.forEachIndexed { index, part ->
        if (index % 2 == 0) {
            append(part)
        } else {
            if (parts[index - 1].lastOrNull()?.isWhitespace() == false) append(WORD_JOINER)
            appendEnglish(part)
            if (parts[index + 1].firstOrNull()?.isWhitespace() == false) append(WORD_JOINER)
        }
    }
}

private const val WORD_JOINER = '⁠'
