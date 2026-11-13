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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.LocaleList
import com.gyugle.gil.core.designsystem.component.AnswerMark
import com.gyugle.gil.core.designsystem.component.AnswerOption
import com.gyugle.gil.core.designsystem.component.AnswerOptionList
import com.gyugle.gil.core.designsystem.component.BlankSentenceText
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.KoreanProse
import com.gyugle.gil.core.designsystem.component.SoundButton
import com.gyugle.gil.feature.review.impl.presentation.R

/** The prompt of a card and its options or answer field. */
@Composable
internal fun ColumnScope.CardContent(
    card: CardUi,
    hasVoice: Boolean,
    answered: Boolean,
    onAction: (ReviewAction) -> Unit
) {
    when (card) {
        is CardUi.Choice -> ChoiceContent(card, hasVoice, onAction)
        is CardUi.Typed -> TypedContent(card, hasVoice, answered, onAction)
    }
}

@Composable
private fun ColumnScope.ChoiceContent(card: CardUi.Choice, hasVoice: Boolean, onAction: (ReviewAction) -> Unit) {
    val play = { onAction(ReviewAction.OnPlayClick) }
    val playDescription = stringResource(R.string.review_play)
    when (val prompt = card.prompt) {
        // The word and its IPA read as one; the speaker sits beside the word, as in a lesson.
        is ChoicePromptUi.Meaning -> Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                EnglishText(text = prompt.english, size = EnglishTextSize.Focus)
                // The word is read just above.
                prompt.ipa?.let { IpaText(ipa = it, spokenWord = null) }
            }
            if (prompt.speakable) SoundButton(onClick = play, description = playDescription)
        }

        is ChoicePromptUi.English -> KoreanProse(text = prompt.meaningKo, style = MaterialTheme.typography.titleLarge)

        is ChoicePromptUi.Listen -> {
            if (hasVoice) SoundButton(onClick = play, description = playDescription, large = true)
            prompt.revealed?.let { EnglishText(text = it, size = EnglishTextSize.Focus) }
        }

        is ChoicePromptUi.Letter -> {
            if (hasVoice) SoundButton(onClick = play, description = playDescription, large = true)
            // Without a voice the name is the whole question.
            if (prompt.showsIpa) IpaText(ipa = prompt.nameIpa, spokenWord = null, prominent = !hasVoice)
        }

        is ChoicePromptUi.Sound -> IpaText(ipa = prompt.symbol, spokenWord = null, prominent = true)
    }
    AnswerOptionList(count = card.options.size) { index, modifier ->
        val option = card.options[index]
        val mark = when {
            card.correct == null -> AnswerMark.IDLE
            index == card.correct -> AnswerMark.CORRECT
            index == card.picked -> AnswerMark.PICKED_WRONG
            else -> AnswerMark.OTHER
        }
        AnswerOption(
            text = option.text,
            english = option.english,
            ipa = option.ipa,
            mark = mark,
            onClick = { onAction(ReviewAction.OnOptionClick(index)) },
            modifier = modifier
        )
    }
}

@Composable
private fun ColumnScope.TypedContent(
    card: CardUi.Typed,
    hasVoice: Boolean,
    answered: Boolean,
    onAction: (ReviewAction) -> Unit
) {
    when (val prompt = card.prompt) {
        is TypedPromptUi.Spell -> {
            if (prompt.speakable && hasVoice) {
                SoundButton(
                    onClick = { onAction(ReviewAction.OnPlayClick) },
                    description = stringResource(R.string.review_play),
                    large = true
                )
            }
            Column {
                KoreanProse(text = prompt.meaningKo, style = MaterialTheme.typography.titleLarge)
                // Read aloud, the word would give the spelling away.
                prompt.ipa?.let { IpaText(ipa = it, spokenWord = null) }
            }
        }

        is TypedPromptUi.Blank -> {
            BlankSentenceText(text = prompt.text)
            prompt.translation?.let { KoreanProse(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    OutlinedTextField(
        value = card.text,
        onValueChange = { onAction(ReviewAction.OnTextChange(it)) },
        modifier = Modifier.fillMaxWidth(),
        // Read-only rather than disabled once checked, so the answer stays readable next to the right one.
        readOnly = answered,
        label = { Text(stringResource(R.string.review_answer_label)) },
        // Typed English is read back in English by screen readers.
        textStyle = MaterialTheme.typography.bodyLarge.copy(localeList = EnglishLocale),
        singleLine = true,
        // Suggestions would hand over the spelling being practised. A password keyboard shows
        // none and learns nothing; without a visual transformation the text stays visible.
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = { onAction(ReviewAction.OnCheckClick) })
    )
}

private val EnglishLocale = LocaleList("en-US")
