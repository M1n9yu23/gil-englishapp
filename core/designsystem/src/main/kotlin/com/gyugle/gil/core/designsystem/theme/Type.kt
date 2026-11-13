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
package com.gyugle.gil.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import com.gyugle.gil.core.designsystem.R

internal val Pretendard = FontFamily(
    Font(R.font.pretendard_regular, FontWeight.Normal),
    Font(R.font.pretendard_medium, FontWeight.Medium),
    Font(R.font.pretendard_semibold, FontWeight.SemiBold),
    Font(R.font.pretendard_bold, FontWeight.Bold)
)

/** English sentences and IPA only. Everything else, including headings, is set in Pretendard. */
internal val Charis = FontFamily(
    Font(R.font.charis_regular, FontWeight.Normal),
    Font(R.font.charis_bold, FontWeight.Bold)
)

// Korean breaks between words, not inside them (keep-all). Android applies this from API 33 when
// the text locale is Korean; older versions fall back to breaking between syllables.
private val KoreanLineBreak = LineBreak.Paragraph.copy(wordBreak = LineBreak.WordBreak.Phrase)

private fun pretendard(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = Pretendard,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    lineBreak = KoreanLineBreak
)

private val DisplaySmall = pretendard(32, 40, FontWeight.SemiBold)
private val HeadlineSmall = pretendard(24, 32, FontWeight.SemiBold)
private val TitleLarge = pretendard(20, 28, FontWeight.SemiBold)
private val TitleMedium = pretendard(16, 24, FontWeight.SemiBold)
private val BodyLarge = pretendard(16, 26, FontWeight.Normal)
private val BodyMedium = pretendard(14, 22, FontWeight.Normal)
private val LabelLarge = pretendard(14, 20, FontWeight.Medium)
private val LabelMedium = pretendard(12, 16, FontWeight.Medium)

/**
 * Figures in stats and times: the same size, at label weight, with digits of one width so numbers
 * in a column line up and a changing count does not shift its neighbours.
 */
fun TextStyle.tabularFigures(): TextStyle = copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum")

// Eight styles. The slots Gil does not design for reuse the nearest one, so a Material component
// that reads them still gets Pretendard at a size from the scale.

/**
 * The app's type scale. Inside [GilTheme], read it through `MaterialTheme.typography`; it is public
 * for surfaces Compose UI does not draw, such as the home screen widget, which take its sizes.
 */
val GilTypography = Typography(
    displayLarge = DisplaySmall,
    displayMedium = DisplaySmall,
    displaySmall = DisplaySmall,
    headlineLarge = HeadlineSmall,
    headlineMedium = HeadlineSmall,
    headlineSmall = HeadlineSmall,
    titleLarge = TitleLarge,
    titleMedium = TitleMedium,
    titleSmall = LabelLarge,
    bodyLarge = BodyLarge,
    bodyMedium = BodyMedium,
    bodySmall = LabelMedium,
    labelLarge = LabelLarge,
    labelMedium = LabelMedium,
    labelSmall = LabelMedium
)
