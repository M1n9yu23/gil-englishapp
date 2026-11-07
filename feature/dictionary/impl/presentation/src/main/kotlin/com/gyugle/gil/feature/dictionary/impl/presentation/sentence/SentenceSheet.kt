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
package com.gyugle.gil.feature.dictionary.impl.presentation.sentence

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gyugle.gil.core.designsystem.R as CoreR
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.SoundButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.rememberContentReporter
import com.gyugle.gil.feature.dictionary.impl.presentation.R
import com.gyugle.gil.feature.dictionary.impl.presentation.component.ContentErrorView

private object SentenceSheetTokens {
    val RoleWidth = 56.dp

    // Lines the panel's text up with the label inside its text button below.
    val PanelStart = 16.dp
    val PlaceholderHeight = 14.dp
    val LoadingHeight = 120.dp

    /** Keeps an error in the sheet from stretching it to the whole screen. */
    val ErrorMaxHeight = 360.dp
}

@Composable
internal fun SentenceRoot(
    sentenceId: Long,
    source: String,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    viewModel: SentenceViewModel = hiltViewModel<SentenceViewModel, SentenceViewModel.Factory>(
        creationCallback = { factory -> factory.create(sentenceId) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val reporter = rememberContentReporter()
    SentenceSheet(
        state = state,
        onAction = viewModel::onAction,
        onBack = onBack,
        onOpenEntry = onOpenEntry,
        onReport = if (reporter.canReport) ({ sentence -> reporter.report(source, sentence.reportText()) }) else null
    )
}

/** The sentence and its translation, as a content report quotes them. */
private fun SentenceUi.reportText(): String = listOfNotNull(text, ko).joinToString("\n")

/** The content of the sentence sheet. [onReport] is null when no app can open the report form. */
@Composable
internal fun SentenceSheet(
    state: SentenceState,
    onAction: (SentenceAction) -> Unit,
    onBack: () -> Unit,
    onOpenEntry: (String) -> Unit,
    onReport: ((SentenceUi) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val spacing = GilTheme.spacing
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.dictionary_sentence_title),
            modifier = Modifier
                .padding(horizontal = spacing.screenMargin)
                .semantics { heading() },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val sentence = state.sentence
        val error = state.error
        when {
            error != null -> ContentErrorView(
                error = error,
                onRetry = { onAction(SentenceAction.OnRetryClick) },
                modifier = Modifier.heightIn(max = SentenceSheetTokens.ErrorMaxHeight),
                onBack = onBack
            )

            state.isLoading || sentence == null -> SentenceLoading()

            else -> Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(start = spacing.screenMargin, end = spacing.screenMargin, bottom = spacing.sectionGap)
            ) {
                SentenceContent(sentence, state, onAction, onOpenEntry)
                if (onReport != null) {
                    TextActionButton(
                        text = stringResource(R.string.dictionary_report),
                        onClick = { onReport(sentence) },
                        modifier = Modifier.padding(top = spacing.sectionGap)
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.SentenceContent(
    sentence: SentenceUi,
    state: SentenceState,
    onAction: (SentenceAction) -> Unit,
    onOpenEntry: (String) -> Unit
) {
    val spacing = GilTheme.spacing
    Row(modifier = Modifier.padding(top = spacing.itemGap), verticalAlignment = Alignment.Top) {
        EnglishText(
            text = linkedSentence(sentence, state.selected) { onAction(SentenceAction.OnWordClick(it)) },
            size = EnglishTextSize.Focus,
            modifier = Modifier.weight(1f)
        )
        if (state.hasVoice) {
            SoundButton(
                onClick = { onAction(SentenceAction.OnPlayClick) },
                description = stringResource(R.string.dictionary_play_sentence)
            )
        }
    }
    sentence.ko?.let {
        KoreanProse(
            text = it,
            modifier = Modifier.padding(top = spacing.inlineGap),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (sentence.words.isNotEmpty()) {
        val word = state.selected?.let { sentence.words.getOrNull(it) }
        if (word == null) {
            Text(
                text = stringResource(R.string.dictionary_sentence_tap_hint),
                modifier = Modifier.padding(top = spacing.itemGap),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            WordPanel(word, onOpen = { onOpenEntry(word.wordId) })
        }
    }
    if (sentence.chunks.isNotEmpty()) {
        SheetSection(stringResource(R.string.dictionary_sentence_chunks))
        sentence.chunks.forEach { chunk ->
            Row(
                modifier = Modifier.padding(bottom = spacing.itemGap),
                horizontalArrangement = Arrangement.spacedBy(spacing.itemGap)
            ) {
                Text(
                    text = stringResource(chunk.role),
                    modifier = Modifier
                        .widthIn(min = SentenceSheetTokens.RoleWidth)
                        .alignByBaseline(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(modifier = Modifier.weight(1f).alignByBaseline()) {
                    EnglishText(text = chunk.text)
                    chunk.glossKo?.let {
                        KoreanProse(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
    sentence.literalKo?.let {
        SheetSection(stringResource(R.string.dictionary_sentence_literal))
        KoreanProse(text = it)
    }
    sentence.grammar?.let { grammar ->
        SheetSection(stringResource(R.string.dictionary_sentence_grammar, grammar.title))
        grammar.paragraphs.forEachIndexed { index, paragraph ->
            KoreanProse(text = paragraph, modifier = Modifier.padding(top = if (index > 0) spacing.inlineGap else 0.dp))
        }
    }
}

/**
 * The sentence with each dictionary word underlined as a link screen readers also offer. The
 * tapped word is marked with the selection colour.
 */
@Composable
private fun linkedSentence(sentence: SentenceUi, selected: Int?, onWordClick: (Int) -> Unit): AnnotatedString {
    val selectedColor = MaterialTheme.colorScheme.primaryContainer
    val pressedColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val currentOnWordClick by rememberUpdatedState(onWordClick)
    return remember(sentence, selected, selectedColor, pressedColor) {
        buildAnnotatedString {
            append(sentence.text)
            sentence.words.forEachIndexed { index, word ->
                val styles = TextLinkStyles(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline,
                        background = if (index == selected) selectedColor else Color.Unspecified
                    ),
                    pressedStyle = SpanStyle(background = pressedColor)
                )
                addLink(
                    LinkAnnotation.Clickable(tag = "word-$index", styles = styles) {
                        currentOnWordClick(index)
                    },
                    word.start,
                    word.end
                )
            }
        }
    }
}

/** What the tapped word means in this sentence, and the way to its page. */
@Composable
private fun WordPanel(word: SentenceWordUi, onOpen: () -> Unit) {
    val spacing = GilTheme.spacing
    Column(
        modifier = Modifier
            .padding(top = spacing.itemGap)
            .fillMaxWidth()
            // Tapping a word changes this panel; a screen reader hears the new meaning.
            .semantics { liveRegion = LiveRegionMode.Polite }
            .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.medium)
            .padding(start = SentenceSheetTokens.PanelStart, end = spacing.inlineGap, top = spacing.itemGap)
    ) {
        EnglishText(text = word.headword)
        if (word.meaningKo != null && word.partOfSpeech != null) {
            Text(
                text = stringResource(word.partOfSpeech),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            KoreanProse(text = word.meaningKo, style = MaterialTheme.typography.titleMedium)
        } else {
            Text(
                text = stringResource(R.string.dictionary_sentence_no_sense),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextActionButton(
            text = stringResource(R.string.dictionary_sentence_open_word),
            onClick = onOpen,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
private fun SheetSection(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .padding(top = GilTheme.spacing.sectionGap, bottom = GilTheme.spacing.itemGap)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Two lines where the sentence and its translation will be, read as one node. */
@Composable
private fun SentenceLoading() {
    val spacing = GilTheme.spacing
    val label = stringResource(CoreR.string.common_loading)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SentenceSheetTokens.LoadingHeight)
            .padding(horizontal = spacing.screenMargin, vertical = spacing.itemGap)
            .clearAndSetSemantics { contentDescription = label },
        verticalArrangement = Arrangement.spacedBy(spacing.itemGap)
    ) {
        listOf(0.8f, 0.5f).forEach { fraction ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(SentenceSheetTokens.PlaceholderHeight)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun SentenceSheetPreview() {
    GilTheme {
        Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
            SentenceSheet(
                state = SentenceState(
                    isLoading = false,
                    sentence = SentencePreviewData.sentence,
                    selected = 1,
                    hasVoice = true
                ),
                onAction = {},
                onBack = {},
                onOpenEntry = {},
                onReport = {}
            )
        }
    }
}
