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
package com.gyugle.gil.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.sp
import com.gyugle.gil.core.designsystem.R
import com.gyugle.gil.core.designsystem.theme.Charis
import com.gyugle.gil.core.designsystem.theme.GilTheme

private val EnglishLocale = LocaleList("en-US")

// The locale has to sit on a span, not only on the TextStyle: Compose turns span locales into the
// LocaleSpan that TalkBack uses to pick an English voice.
private val EnglishSpan = SpanStyle(fontFamily = Charis, localeList = EnglishLocale)

enum class EnglishTextSize {
    /** The sentence a lesson step is about. */
    Focus,

    /** Examples, answers and list content. */
    Body
}

/**
 * Every English sentence the learner reads goes through here: set in a serif so it stands apart
 * from Korean at a glance, and marked en-US so screen readers pronounce it in English.
 * English inside a Korean sentence uses [appendEnglish] instead.
 */
@Composable
fun EnglishText(
    text: String,
    modifier: Modifier = Modifier,
    size: EnglishTextSize = EnglishTextSize.Body,
    color: Color = Color.Unspecified,
    textDecoration: TextDecoration? = null
) {
    val annotated = remember(text) { buildAnnotatedString { appendEnglish(text) } }
    Text(text = annotated, modifier = modifier, color = color, textDecoration = textDecoration, style = size.style())
}

/**
 * [EnglishText] for a sentence that carries its own spans or links, such as words the learner can
 * tap. The whole text is set and read as English.
 */
@Composable
fun EnglishText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    size: EnglishTextSize = EnglishTextSize.Body,
    color: Color = Color.Unspecified
) {
    val annotated = remember(text) { buildAnnotatedString { withStyle(EnglishSpan) { append(text) } } }
    Text(text = annotated, modifier = modifier, color = color, style = size.style())
}

/**
 * [EnglishText] for a sentence with [BLANK] where a word is missing. Screen readers skip
 * underscores, so they read the gap as the word "blank" in the app's language instead.
 */
@Composable
fun BlankSentenceText(text: String, modifier: Modifier = Modifier, size: EnglishTextSize = EnglishTextSize.Focus) {
    val blank = stringResource(R.string.blank_spoken)
    val spoken = remember(text, blank) { spokenBlanks(text, blank, LocaleList.current) }
    EnglishText(text = text, modifier = modifier.clearAndSetSemantics { this.text = spoken }, size = size)
}

/** The gap in a fill-in-the-blank sentence. */
const val BLANK = "___"

internal fun spokenBlanks(text: String, blank: String, blankLocale: LocaleList): AnnotatedString =
    buildAnnotatedString {
        text.split(BLANK).forEachIndexed { index, part ->
            if (index > 0) withStyle(SpanStyle(localeList = blankLocale)) { append(blank) }
            appendEnglish(part)
        }
    }

/** English inside surrounding text: same size as its neighbours, Charis, read in English. */
fun AnnotatedString.Builder.appendEnglish(text: String) {
    withStyle(EnglishSpan) { append(text) }
}

/**
 * A pronunciation such as `ˈwɔːtər`, shown between slashes. Screen readers cannot read IPA
 * symbols, so they read [spokenWord], the English word it spells, with an English voice. With a
 * null [spokenWord] they skip it: where the word is already read out, or must not be given away.
 * [prominent] sets it at the size of a focus sentence, for a question that is the IPA itself.
 */
@Composable
fun IpaText(ipa: String, spokenWord: String?, modifier: Modifier = Modifier, prominent: Boolean = false) {
    val spoken = remember(spokenWord) { spokenWord?.let { buildAnnotatedString { appendEnglish(it) } } }
    Text(
        text = "/$ipa/",
        modifier = modifier.clearAndSetSemantics { if (spoken != null) text = spoken },
        color = if (prominent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        style = if (prominent) FocusStyle else IpaStyle
    )
}

private fun EnglishTextSize.style(): TextStyle = when (this) {
    EnglishTextSize.Focus -> FocusStyle
    EnglishTextSize.Body -> BodyStyle
}

// The focus sentence is short and large; balanced lines avoid a lone last word.
private val FocusStyle = englishStyle(size = 26, lineHeight = 34).copy(lineBreak = LineBreak.Heading)
private val BodyStyle = englishStyle(size = 18, lineHeight = 28)
private val IpaStyle = englishStyle(size = 16, lineHeight = 24)

private fun englishStyle(size: Int, lineHeight: Int) = TextStyle(
    fontFamily = Charis,
    fontWeight = FontWeight.Normal,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    localeList = EnglishLocale,
    hyphens = Hyphens.None
)

@PreviewLightDark
@Composable
private fun EnglishTextPreview() {
    GilTheme {
        Surface {
            Column(
                modifier = Modifier.padding(GilTheme.spacing.screenMargin),
                verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)
            ) {
                EnglishText(text = "Could I get a glass of water?", size = EnglishTextSize.Focus)
                EnglishText(text = "I usually walk to work.")
                IpaText(ipa = "ˈwɔːtər", spokenWord = "water")
                Text(
                    text = buildAnnotatedString {
                        append("물을 달라고 할 때는 ")
                        appendEnglish("Could I get")
                        append("으로 시작해요.")
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
