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
package com.gyugle.gil.feature.dictionary.impl.presentation.entry

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.PressableSurface
import com.gyugle.gil.core.designsystem.icon.GilIcons
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.dictionary.impl.presentation.R

private object EntryPageTokens {
    val RowMinHeight = 48.dp

    /** The width an icon button takes, so plain icons line up with the buttons above them. */
    val TrailingSlot = 48.dp
}

@Composable
internal fun ColumnScope.WordPage(
    word: EntryUi.Word,
    voices: VoicesUi,
    onAction: (EntryAction) -> Unit,
    navigation: EntryNavigation
) {
    Headline(text = word.text, ipa = word.ipa, voices = voices, onAction = onAction)
    word.hangul?.let {
        Text(
            text = stringResource(R.string.dictionary_hangul_aid, it),
            modifier = Modifier.padding(top = GilTheme.spacing.inlineGap),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    SectionTitle(
        stringResource(if (word.learnedAny) R.string.dictionary_senses_now else R.string.dictionary_senses_first)
    )
    word.now.forEachIndexed { index, sense ->
        if (index > 0) HorizontalDivider(modifier = Modifier.padding(vertical = GilTheme.spacing.itemGap))
        SenseBlock(sense, voices, onAction) { navigation.onOpenSentence(it, word.id) }
    }
    if (word.later.isNotEmpty()) LaterSenses(word)
    if (word.collocations.isNotEmpty()) {
        SectionTitle(stringResource(R.string.dictionary_collocations))
        word.collocations.forEach { PhraseRow(it) }
    }
    if (word.family.isNotEmpty()) {
        SectionTitle(stringResource(R.string.dictionary_family))
        word.family.forEach { FamilyRow(it, navigation.onOpenEntry) }
    }
    if (word.notes.isNotEmpty()) {
        SectionTitle(stringResource(R.string.dictionary_notes))
        word.notes.forEach { NoteBlock(it) }
    }
}

@Composable
internal fun ColumnScope.PatternPage(
    pattern: EntryUi.Pattern,
    voices: VoicesUi,
    onAction: (EntryAction) -> Unit,
    navigation: EntryNavigation
) {
    Headline(text = pattern.text, ipa = null, voices = voices, onAction = onAction)
    SectionTitle(stringResource(R.string.dictionary_meaning))
    MeaningLine(
        label = null,
        meaningKo = pattern.meaningKo,
        saved = pattern.saved,
        onToggle = { onAction(EntryAction.OnSaveToggle(pattern.id)) }
    )
    pattern.noteKo?.let {
        KoreanProse(text = it, modifier = Modifier.padding(top = GilTheme.spacing.inlineGap))
    }
    pattern.examples.forEach { ExampleRow(it, voices, onAction) { id -> navigation.onOpenSentence(id, pattern.id) } }
}

@Composable
internal fun ColumnScope.SoundPage(sound: EntryUi.Sound, voices: VoicesUi, onAction: (EntryAction) -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = GilTheme.spacing.inlineGap),
            verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)
        ) {
            EnglishText(
                text = sound.spelling,
                size = EnglishTextSize.Focus,
                modifier = Modifier.semantics { heading() }
            )
            IpaText(ipa = sound.symbol, spokenWord = null)
        }
        SaveToggle(name = sound.spelling, saved = sound.saved, onToggle = {
            onAction(EntryAction.OnSaveToggle(sound.id))
        })
    }
    KoreanProse(text = sound.descriptionKo, modifier = Modifier.padding(top = GilTheme.spacing.itemGap))
    if (sound.examples.isNotEmpty()) {
        SectionTitle(stringResource(R.string.dictionary_sound_examples))
        sound.examples.forEach { example ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = EntryPageTokens.RowMinHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)
            ) {
                EnglishText(text = example.text)
                IpaText(ipa = example.ipa, spokenWord = null, modifier = Modifier.weight(1f))
                if (voices.american) {
                    PlayButton(text = example.text, onClick = {
                        onAction(EntryAction.OnPlayClick(example.text))
                    })
                }
            }
        }
    }
}

/** The headword, its IPA and a play button for each accent that has a voice. */
@Composable
private fun ColumnScope.Headline(text: String, ipa: String?, voices: VoicesUi, onAction: (EntryAction) -> Unit) {
    val spacing = GilTheme.spacing
    EnglishText(
        text = text,
        size = EnglishTextSize.Focus,
        modifier = Modifier
            .padding(top = spacing.inlineGap)
            .semantics { heading() }
    )
    FlowRow(
        modifier = Modifier.padding(top = spacing.inlineGap),
        horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        // The headword above is read in English; its IPA would be read symbol by symbol.
        ipa?.let { IpaText(ipa = it, spokenWord = null) }
        if (voices.american) {
            AccentButton(
                label = stringResource(R.string.dictionary_accent_american),
                onClick = { onAction(EntryAction.OnPlayClick(text, Accent.AMERICAN)) }
            )
        }
        if (voices.british) {
            AccentButton(
                label = stringResource(R.string.dictionary_accent_british),
                onClick = { onAction(EntryAction.OnPlayClick(text, Accent.BRITISH)) }
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .padding(top = GilTheme.spacing.sectionGap, bottom = GilTheme.spacing.itemGap)
            .semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun ColumnScope.SenseBlock(
    sense: SenseUi,
    voices: VoicesUi,
    onAction: (EntryAction) -> Unit,
    onOpenSentence: (Long) -> Unit
) {
    MeaningLine(
        label = stringResource(sense.partOfSpeech),
        meaningKo = sense.meaningKo,
        saved = sense.saved,
        onToggle = { onAction(EntryAction.OnSaveToggle(sense.id)) }
    )
    sense.definitionEn?.let {
        EnglishText(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    sense.ipa?.let { IpaText(ipa = it, spokenWord = null) }
    sense.examples.forEach { ExampleRow(it, voices, onAction, onOpenSentence) }
}

/** A part of speech over a meaning, with the word list toggle beside them. */
@Composable
private fun MeaningLine(label: String?, meaningKo: String, saved: Boolean, onToggle: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Read as one, before the toggle that sits beside both lines.
        Column(modifier = Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            label?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            KoreanProse(text = meaningKo, style = MaterialTheme.typography.titleMedium)
        }
        SaveToggle(name = meaningKo, saved = saved, onToggle = onToggle)
    }
}

/** An example sentence that opens its breakdown, with a play button when there is a voice. */
@Composable
private fun ExampleRow(
    example: ExampleUi,
    voices: VoicesUi,
    onAction: (EntryAction) -> Unit,
    onOpenSentence: (Long) -> Unit
) {
    Row(modifier = Modifier.padding(top = GilTheme.spacing.inlineGap), verticalAlignment = Alignment.CenterVertically) {
        PressableSurface(
            onClick = { onOpenSentence(example.sentenceId) },
            clickLabel = stringResource(R.string.dictionary_take_apart),
            modifier = Modifier.weight(1f)
        ) {
            Column(modifier = Modifier.padding(vertical = GilTheme.spacing.inlineGap)) {
                EnglishText(text = example.text)
                example.ko?.let {
                    KoreanProse(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (voices.american) {
            PlayButton(text = example.text, onClick = {
                onAction(EntryAction.OnPlayClick(example.text))
            })
        }
    }
}

/** The meanings for later, folded away so they do not crowd out the ones to know now. */
@Composable
private fun ColumnScope.LaterSenses(word: EntryUi.Word) {
    val later = word.later
    val spacing = GilTheme.spacing
    var expanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(if (expanded) R.string.dictionary_expanded else R.string.dictionary_collapsed)
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(GilTheme.motion.fade),
        label = "expand"
    )
    Spacer(Modifier.height(spacing.sectionGap))
    PressableSurface(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { stateDescription = state }
    ) {
        Row(
            modifier = Modifier.heightIn(min = EntryPageTokens.RowMinHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pluralStringResource(R.plurals.dictionary_senses_later, later.size, later.size),
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            TrailingIcon {
                Icon(
                    painter = GilIcons.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.rotate(rotation),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    AnimatedVisibility(visible = expanded) {
        Column(
            verticalArrangement = Arrangement.spacedBy(spacing.itemGap),
            modifier = Modifier.padding(top = spacing.inlineGap)
        ) {
            later.forEach { sense ->
                Column {
                    Text(
                        text = stringResource(sense.partOfSpeech),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    KoreanProse(text = sense.meaningKo)
                    Text(
                        text = stringResource(R.string.dictionary_taught_in, sense.stage.asString()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PhraseRow(phrase: PhraseUi) {
    Column(modifier = Modifier.padding(bottom = GilTheme.spacing.itemGap)) {
        EnglishText(text = phrase.text)
        KoreanProse(
            text = phrase.ko,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A related word; one the dictionary has opens its page. */
@Composable
private fun FamilyRow(word: FamilyWordUi, onOpenEntry: (String) -> Unit) {
    val target = word.entryId
    if (target == null) {
        EnglishText(text = word.text, modifier = Modifier.padding(vertical = GilTheme.spacing.inlineGap))
    } else {
        PressableSurface(
            onClick = { onOpenEntry(target) },
            clickLabel = stringResource(R.string.dictionary_open_entry),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.heightIn(min = EntryPageTokens.RowMinHeight),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EnglishText(text = word.text, modifier = Modifier.weight(1f))
                TrailingIcon {
                    Icon(
                        painter = GilIcons.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteBlock(note: NoteUi) {
    Column(modifier = Modifier.padding(bottom = GilTheme.spacing.itemGap)) {
        Text(
            text = stringResource(note.label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        KoreanProse(text = note.ko)
    }
}

/** An icon in the 48dp slot an icon button takes, so all icons on the right share one centre line. */
@Composable
private fun TrailingIcon(icon: @Composable () -> Unit) {
    Box(modifier = Modifier.size(EntryPageTokens.TrailingSlot), contentAlignment = Alignment.Center) { icon() }
}
