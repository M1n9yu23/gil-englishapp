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
package com.gyugle.gil.feature.widget.impl.presentation

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.fontSize
import com.gyugle.gil.core.designsystem.theme.DarkColorScheme
import com.gyugle.gil.core.designsystem.theme.GilTypography
import com.gyugle.gil.core.designsystem.theme.LightColorScheme
import com.gyugle.gil.core.designsystem.theme.Spacing
import com.gyugle.gil.feature.home.api.TODAY_DEEP_LINK
import com.gyugle.gil.feature.review.api.REVIEW_DEEP_LINK

/** One line: the review row alone. */
internal val ROW_SIZE = DpSize(180.dp, 40.dp)

/** Tall enough for the review row and the day's sentence under it. */
internal val FULL_SIZE = DpSize(180.dp, 100.dp)

/** The app's paper-and-ink colours, light or dark with the phone. */
private val WidgetColors = ColorProviders(light = LightColorScheme, dark = DarkColorScheme)

/** Glance's colour set has no outlineVariant, the colour of the app's dividers. */
private val DividerColor = ColorProvider(day = LightColorScheme.outlineVariant, night = DarkColorScheme.outlineVariant)

/** The widget is drawn outside GilTheme, so it takes the token defaults. */
private val spacing = Spacing()

/** The inner padding launchers give widgets from Android 12 (system_app_widget_internal_padding). */
private val WidgetPadding = 16.dp

/** The smallest touch target, for the review row. */
private val RowHeight = 48.dp

/** As thin as the app's dividers. */
private val DividerThickness = 1.dp

/**
 * The widget: today's reviews on top and, when the widget is tall enough, the day's sentence. Tapping
 * the reviews opens them, tapping the sentence opens its breakdown, and anywhere else opens Today.
 */
@Composable
internal fun TodayWidgetLayout(state: TodayWidgetState) {
    GlanceTheme(colors = WidgetColors) {
        val full = LocalSize.current.height >= FULL_SIZE.height
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .appWidgetBackground()
                .background(GlanceTheme.colors.surface)
                .systemCorners()
                .opens(TODAY_DEEP_LINK)
                .padding(horizontal = WidgetPadding),
            verticalAlignment = if (full) Alignment.Top else Alignment.CenterVertically
        ) {
            when (state) {
                is TodayWidgetState.Ready -> {
                    ReviewRow(state.dueCount)
                    if (full) {
                        Spacer(GlanceModifier.fillMaxWidth().height(DividerThickness).background(DividerColor))
                        SentenceBlock(state.sentence)
                    }
                }

                TodayWidgetState.Unavailable -> Text(
                    text = LocalContext.current.getString(R.string.widget_unavailable),
                    style = bodyStyle(),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(dueCount: Int) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(RowHeight)
            .opens(if (dueCount > 0) REVIEW_DEEP_LINK else TODAY_DEEP_LINK),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (dueCount > 0) {
            Text(
                text = context.getString(R.string.widget_review_title),
                style = labelStyle(),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = context.resources.getQuantityString(R.plurals.widget_review_count, dueCount, dueCount),
                style = labelStyle().copy(color = GlanceTheme.colors.primary),
                maxLines = 1
            )
        } else {
            Text(text = context.getString(R.string.widget_review_none), style = labelStyle(), maxLines = 1)
        }
    }
}

@Composable
private fun ColumnScope.SentenceBlock(sentence: SentenceUi?) {
    if (sentence == null) {
        Text(
            text = LocalContext.current.getString(R.string.widget_sentence_empty),
            style = bodyStyle(),
            maxLines = 2,
            modifier = GlanceModifier.padding(vertical = spacing.itemGap)
        )
        return
    }
    Column(
        modifier = GlanceModifier.fillMaxWidth().defaultWeight().opens(
            sentence.link
        ).padding(vertical = spacing.itemGap)
    ) {
        Text(
            text = sentence.text,
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = EnglishTextSize.Body.fontSize,
                fontFamily = FontFamily.Serif // design-allow(design.text.font-family): widgets get system fonts only
            ),
            maxLines = 2
        )
        if (sentence.ko != null) {
            Spacer(GlanceModifier.height(spacing.inlineGap / 2))
            Text(text = sentence.ko, style = bodyStyle(), maxLines = 1)
        }
    }
}

@Composable
private fun labelStyle() = TextStyle(
    color = GlanceTheme.colors.onSurface,
    fontSize = GilTypography.labelLarge.fontSize,
    fontWeight = FontWeight.Medium
)

@Composable
private fun bodyStyle() =
    TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = GilTypography.bodyMedium.fontSize)

/** The launcher's own corner radius, from Android 12; before it, launchers clip widgets themselves. */
private fun GlanceModifier.systemCorners(): GlanceModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    cornerRadius(android.R.dimen.system_app_widget_background_radius)
} else {
    this
}

/** Opens the app at [link], or in the running app when it is already open, as a reminder does. */
@Composable
private fun GlanceModifier.opens(link: String): GlanceModifier {
    val intent = linkIntent(LocalContext.current, link) ?: return this
    return clickable(actionStartActivity(intent))
}

internal fun linkIntent(context: Context, link: String): Intent? =
    context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        data = link.toUri()
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
