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
package com.gyugle.gil.catalog

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.component.EmptyState
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.ErrorState
import com.gyugle.gil.core.designsystem.component.GilProgressBar
import com.gyugle.gil.core.designsystem.component.GilTopBar
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.ListRow
import com.gyugle.gil.core.designsystem.component.LoadingState
import com.gyugle.gil.core.designsystem.component.PrimaryActionButton
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.component.appendEnglish
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme

private object GalleryTokens {
    val SwatchSize = 48.dp
    val SwatchBorder = 1.dp
    val SpacingBarHeight = 12.dp
    val StateFrameHeight = 360.dp
}

/** Every token and role on one scrolling screen, for reviewing a change to the design system. */
@Composable
fun GalleryScreen(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        GilTopBar(title = stringResource(R.string.catalog_title))
        LazyColumn(contentPadding = WindowInsets.navigationBars.asPaddingValues()) {
            item { GallerySection(R.string.catalog_section_colors) { ColorTokens() } }
            item { GallerySection(R.string.catalog_section_type) { TypeTokens() } }
            item { GallerySection(R.string.catalog_section_spacing) { SpacingTokens() } }
            item { GallerySection(R.string.catalog_section_buttons) { Buttons() } }
            item { GallerySection(R.string.catalog_section_rows, inset = false) { Rows() } }
            item { GallerySection(R.string.catalog_section_english) { English() } }
            item { GallerySection(R.string.catalog_section_progress) { Progress() } }
            item { GallerySection(R.string.catalog_section_icons) { Icons() } }
            item { GallerySection(R.string.catalog_section_states, inset = false) { States() } }
        }
    }
}

@Composable
internal fun GallerySection(@StringRes title: Int, inset: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    val spacing = GilTheme.spacing
    Column(modifier = Modifier.padding(vertical = spacing.itemGap)) {
        Text(
            text = stringResource(title),
            modifier = Modifier.padding(horizontal = spacing.screenMargin, vertical = spacing.inlineGap),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = if (inset) Modifier.padding(horizontal = spacing.screenMargin) else Modifier,
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
        ) {
            content()
        }
        HorizontalDivider(modifier = Modifier.padding(top = spacing.sectionGap))
    }
}

@Composable
internal fun ColumnScope.ColorTokens() {
    val c = MaterialTheme.colorScheme
    val pairs = listOf(
        Triple("surface", c.surface, c.onSurface),
        Triple("surfaceContainerLow", c.surfaceContainerLow, c.onSurface),
        Triple("surfaceContainer", c.surfaceContainer, c.onSurface),
        Triple("surfaceContainerHigh", c.surfaceContainerHigh, c.onSurfaceVariant),
        Triple("primary", c.primary, c.onPrimary),
        Triple("primaryContainer", c.primaryContainer, c.onPrimaryContainer),
        Triple("secondary", c.secondary, c.onSecondary),
        Triple("secondaryContainer", c.secondaryContainer, c.onSecondaryContainer),
        Triple("tertiary", c.tertiary, c.onTertiary),
        Triple("tertiaryContainer", c.tertiaryContainer, c.onTertiaryContainer),
        Triple("error", c.error, c.onError),
        Triple("errorContainer", c.errorContainer, c.onErrorContainer),
        Triple("inverseSurface", c.inverseSurface, c.inverseOnSurface),
        Triple("outline", c.outline, c.surface),
        Triple("outlineVariant", c.outlineVariant, c.onSurface)
    )
    pairs.forEach { (name, background, foreground) -> Swatch(name, background, foreground) }
}

@Composable
private fun Swatch(name: String, background: Color, foreground: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = GalleryTokens.SwatchSize * 2, height = GalleryTokens.SwatchSize)
                .background(background, MaterialTheme.shapes.small)
                .border(
                    GalleryTokens.SwatchBorder,
                    MaterialTheme.colorScheme.outlineVariant,
                    MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = SWATCH_SAMPLE, style = MaterialTheme.typography.labelLarge, color = foreground)
        }
        Text(text = name, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun ColumnScope.TypeTokens() {
    val t = MaterialTheme.typography
    val styles: List<Pair<String, TextStyle>> = listOf(
        "displaySmall" to t.displaySmall,
        "headlineSmall" to t.headlineSmall,
        "titleLarge" to t.titleLarge,
        "titleMedium" to t.titleMedium,
        "bodyLarge" to t.bodyLarge,
        "bodyMedium" to t.bodyMedium,
        "labelLarge" to t.labelLarge,
        "labelMedium" to t.labelMedium
    )
    val sample = stringResource(R.string.catalog_sample_type)
    styles.forEach { (name, style) ->
        Column {
            Text(text = name, style = t.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = sample, style = style)
        }
    }
}

@Composable
private fun ColumnScope.SpacingTokens() {
    val s = GilTheme.spacing
    listOf(
        "inlineGap" to s.inlineGap,
        "itemGap" to s.itemGap,
        "screenMargin" to s.screenMargin,
        "sectionGap" to s.sectionGap
    ).forEach { (name, size) -> SpacingBar(name, size) }
}

@Composable
private fun SpacingBar(name: String, size: Dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(size)
                .height(GalleryTokens.SpacingBarHeight)
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(text = "$name ${size.value.toInt()}dp", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ColumnScope.Buttons() {
    PrimaryActionButton(
        text = stringResource(R.string.catalog_sample_primary),
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    )
    PrimaryActionButton(
        text = stringResource(R.string.catalog_sample_primary),
        onClick = {},
        modifier = Modifier.fillMaxWidth(),
        enabled = false
    )
    SecondaryActionButton(
        text = stringResource(R.string.catalog_sample_secondary),
        onClick = {},
        modifier = Modifier.fillMaxWidth()
    )
    TextActionButton(text = stringResource(R.string.catalog_sample_text_action), onClick = {})
}

@Composable
private fun Rows() {
    Column {
        ListRow(
            headline = stringResource(R.string.catalog_sample_row_title),
            supporting = stringResource(R.string.catalog_sample_row_supporting),
            onClick = {},
            trailing = { Icon(painter = GilIcons.ChevronRight, contentDescription = null) }
        )
        HorizontalDivider()
        ListRow(
            headline = stringResource(R.string.catalog_sample_row_plain),
            supporting = stringResource(R.string.catalog_sample_row_plain_value)
        )
    }
}

@Composable
private fun ColumnScope.English() {
    EnglishText(text = stringResource(R.string.catalog_sample_english_focus), size = EnglishTextSize.Focus)
    EnglishText(text = stringResource(R.string.catalog_sample_english_body))
    IpaText(
        ipa = stringResource(R.string.catalog_sample_ipa),
        spokenWord = stringResource(R.string.catalog_sample_ipa_word)
    )
    val before = stringResource(R.string.catalog_sample_inline_before)
    val english = stringResource(R.string.catalog_sample_inline_english)
    val after = stringResource(R.string.catalog_sample_inline_after)
    Text(
        text = buildAnnotatedString {
            append(before)
            appendEnglish(english)
            append(after)
        },
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
private fun ColumnScope.Progress() {
    GilProgressBar(progress = 0f)
    GilProgressBar(progress = 0.4f)
    GilProgressBar(progress = 1f)
}

@Composable
private fun ColumnScope.Icons() {
    val icons: List<Pair<String, Painter>> = listOf(
        "ArrowBack" to GilIcons.ArrowBack,
        "Check" to GilIcons.Check,
        "ChevronRight" to GilIcons.ChevronRight,
        "Close" to GilIcons.Close
    )
    icons.forEach { (name, painter) ->
        Row(
            horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painter = painter, contentDescription = null)
            Text(text = name, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ColumnScope.States() {
    Box(modifier = Modifier.height(GalleryTokens.StateFrameHeight)) {
        EmptyState(
            title = stringResource(R.string.catalog_sample_empty_title),
            body = stringResource(R.string.catalog_sample_empty_body),
            actionLabel = stringResource(R.string.catalog_sample_empty_action),
            onAction = {}
        )
    }
    HorizontalDivider()
    Box(modifier = Modifier.height(GalleryTokens.StateFrameHeight)) {
        ErrorState(
            title = stringResource(R.string.catalog_sample_error_title),
            body = stringResource(R.string.catalog_sample_error_body),
            retryLabel = stringResource(R.string.catalog_sample_error_retry),
            onRetry = {}
        )
    }
    HorizontalDivider()
    Box(modifier = Modifier.height(GalleryTokens.StateFrameHeight)) {
        LoadingState()
    }
}

// Hangul and Latin, to judge contrast for both scripts.
private const val SWATCH_SAMPLE = "가 Aa"
