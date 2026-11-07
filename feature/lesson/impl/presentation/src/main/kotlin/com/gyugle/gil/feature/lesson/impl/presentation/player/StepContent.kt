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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import com.gyugle.gil.core.designsystem.component.EnglishText
import com.gyugle.gil.core.designsystem.component.EnglishTextSize
import com.gyugle.gil.core.designsystem.component.IpaText
import com.gyugle.gil.core.designsystem.component.SecondaryActionButton
import com.gyugle.gil.core.designsystem.component.TextActionButton
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.feature.lesson.impl.presentation.R
import com.gyugle.gil.feature.lesson.impl.presentation.component.ChoiceOption
import com.gyugle.gil.feature.lesson.impl.presentation.component.KoreanProse
import com.gyugle.gil.feature.lesson.impl.presentation.component.OptionMark
import com.gyugle.gil.feature.lesson.impl.presentation.component.SoundButton
import com.gyugle.gil.feature.lesson.impl.presentation.component.TileBoard

/** The instruction above a step, or null for a step that explains itself. */
@Composable
internal fun StepUi.instruction(): String? = when (this) {
    is StepUi.Explain -> null

    is StepUi.Listen -> stringResource(R.string.lesson_instruction_listen)

    is StepUi.Shadow -> stringResource(R.string.lesson_instruction_shadow)

    is StepUi.Arrange -> stringResource(R.string.lesson_instruction_arrange)

    is StepUi.Typed -> stringResource(
        when (prompt) {
            is TypedPrompt.Spell -> R.string.lesson_instruction_spell
            TypedPrompt.Dictation -> R.string.lesson_instruction_dictation
            is TypedPrompt.Blank -> R.string.lesson_instruction_fill_blank_type
        }
    )

    is StepUi.Choice -> stringResource(
        when (prompt) {
            is ChoicePrompt.Audio -> R.string.lesson_instruction_listen_choose
            ChoicePrompt.SoundPair -> R.string.lesson_instruction_minimal_pair
            is ChoicePrompt.Blank -> R.string.lesson_instruction_fill_blank_choose
            is ChoicePrompt.Dialogue -> R.string.lesson_instruction_dialogue
            is ChoicePrompt.Passage -> R.string.lesson_instruction_passage
            is ChoicePrompt.English, is ChoicePrompt.Korean -> R.string.lesson_instruction_choose
        }
    )
}

/** The body of the step on screen. [answered] turns the inputs off once the answer is checked. */
@Composable
internal fun StepContent(
    step: StepUi,
    hasVoice: Boolean,
    answered: Boolean,
    onAction: (LessonAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap * 2)) {
        StepBody(step, hasVoice, answered, onAction)
    }
}

@Composable
private fun ColumnScope.StepBody(step: StepUi, hasVoice: Boolean, answered: Boolean, onAction: (LessonAction) -> Unit) {
    when (step) {
        is StepUi.Explain -> ExplainContent(step, hasVoice, onAction)

        is StepUi.Listen -> ListenContent(step, hasVoice, onAction)

        is StepUi.Shadow -> ShadowContent(step, hasVoice, onAction)

        is StepUi.Choice -> ChoiceContent(step, hasVoice, onAction)

        is StepUi.Arrange -> {
            KoreanProse(text = step.ko, style = MaterialTheme.typography.titleLarge)
            TileBoard(
                step = step,
                enabled = !answered,
                onTileClick = { onAction(LessonAction.OnTileClick(it)) },
                onChosenTileClick = { onAction(LessonAction.OnChosenTileClick(it)) }
            )
        }

        is StepUi.Typed -> TypedContent(step, hasVoice, answered, onAction)
    }
}

@Composable
private fun ColumnScope.ExplainContent(step: StepUi.Explain, hasVoice: Boolean, onAction: (LessonAction) -> Unit) {
    step.paragraphs.forEach { KoreanProse(text = it) }
    if (step.examples.isNotEmpty()) {
        Column {
            step.examples.forEachIndexed { index, example ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SentenceRow(example, hasVoice, onPlay = { onAction(LessonAction.OnPlaySentenceClick(example.text)) })
            }
        }
    }
}

/** An example: the sentence, its translation, and a small sound button. */
@Composable
private fun SentenceRow(sentence: SentenceUi, hasVoice: Boolean, onPlay: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = GilTheme.spacing.inlineGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            EnglishText(text = sentence.text)
            sentence.ko?.let {
                KoreanProse(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (hasVoice) SoundButton(onClick = onPlay, description = stringResource(R.string.lesson_play))
    }
}

@Composable
private fun ColumnScope.ListenContent(step: StepUi.Listen, hasVoice: Boolean, onAction: (LessonAction) -> Unit) {
    if (hasVoice) {
        SoundButton(onClick = {
            onAction(LessonAction.OnPlayClick)
        }, description = stringResource(R.string.lesson_play), large = true)
    }
    EnglishText(text = step.sentence.text, size = EnglishTextSize.Focus)
    val ko = step.sentence.ko
    if (ko != null) {
        if (step.isMeaningShown) {
            KoreanProse(text = ko, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            TextActionButton(text = stringResource(R.string.lesson_show_meaning), onClick = {
                onAction(LessonAction.OnShowMeaningClick)
            })
        }
    }
}

@Composable
private fun ColumnScope.ShadowContent(step: StepUi.Shadow, hasVoice: Boolean, onAction: (LessonAction) -> Unit) {
    if (hasVoice) {
        SoundButton(onClick = {
            onAction(LessonAction.OnPlayClick)
        }, description = stringResource(R.string.lesson_play), large = true)
    }
    EnglishText(text = step.sentence.text, size = EnglishTextSize.Focus)
    step.sentence.ko?.let { KoreanProse(text = it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    RecorderPanel(step.recorder, onAction)
}

/** Record and compare, or the reason and the way to allow the microphone. */
@Composable
private fun RecorderPanel(recorder: RecorderUi, onAction: (LessonAction) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val spacing = GilTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.inlineGap)) {
        when (recorder.microphone) {
            MicrophoneUi.GRANTED -> {
                if (recorder.isRecording) {
                    Text(
                        text = stringResource(R.string.lesson_recording),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary
                    )
                    SecondaryActionButton(
                        text = stringResource(R.string.lesson_record_stop),
                        onClick = { onAction(LessonAction.OnStopRecordingClick) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.inlineGap)) {
                        SecondaryActionButton(
                            text = stringResource(
                                if (recorder.hasRecording) R.string.lesson_record_again else R.string.lesson_record
                            ),
                            onClick = { onAction(LessonAction.OnRecordClick) },
                            modifier = Modifier.weight(1f)
                        )
                        if (recorder.hasRecording) {
                            SecondaryActionButton(
                                text = stringResource(R.string.lesson_play_mine),
                                onClick = { onAction(LessonAction.OnPlayMineClick) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                if (recorder.failed) {
                    Text(
                        text = stringResource(R.string.lesson_recording_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.error
                    )
                }
            }

            MicrophoneUi.NOT_ASKED, MicrophoneUi.DENIED -> {
                KoreanProse(
                    text = stringResource(R.string.lesson_mic_reason),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                SecondaryActionButton(
                    text = stringResource(R.string.lesson_mic_allow),
                    onClick = { onAction(LessonAction.OnAllowMicrophoneClick) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            MicrophoneUi.PERMANENTLY_DENIED -> {
                KoreanProse(
                    text = stringResource(R.string.lesson_mic_denied),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant
                )
                TextActionButton(text = stringResource(R.string.lesson_mic_open_settings), onClick = {
                    onAction(LessonAction.OnAllowMicrophoneClick)
                })
            }
        }
    }
}

@Composable
private fun ColumnScope.ChoiceContent(step: StepUi.Choice, hasVoice: Boolean, onAction: (LessonAction) -> Unit) {
    val play = { onAction(LessonAction.OnPlayClick) }
    val playDescription = stringResource(R.string.lesson_play)
    when (val prompt = step.prompt) {
        is ChoicePrompt.English -> Row(verticalAlignment = Alignment.CenterVertically) {
            EnglishText(text = prompt.text, size = EnglishTextSize.Focus, modifier = Modifier.weight(1f))
            if (hasVoice) SoundButton(onClick = play, description = playDescription)
        }

        is ChoicePrompt.Audio -> {
            if (hasVoice) SoundButton(onClick = play, description = playDescription, large = true)
            prompt.text?.let { EnglishText(text = it, size = EnglishTextSize.Focus) }
        }

        is ChoicePrompt.Korean -> KoreanProse(text = prompt.text, style = MaterialTheme.typography.titleLarge)

        ChoicePrompt.SoundPair -> if (hasVoice) SoundButton(onClick = play, description = playDescription, large = true)

        is ChoicePrompt.Blank -> {
            EnglishText(text = prompt.text, size = EnglishTextSize.Focus)
            KoreanProse(text = prompt.ko, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        is ChoicePrompt.Dialogue -> {
            if (hasVoice) {
                SecondaryActionButton(text = stringResource(R.string.lesson_play_conversation), onClick = play)
            }
            prompt.lines.forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)) {
                    Text(
                        text = line.speaker,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    EnglishText(text = line.text, modifier = Modifier.weight(1f))
                }
            }
            prompt.question?.let { KoreanProse(text = it, style = MaterialTheme.typography.titleMedium) }
        }

        is ChoicePrompt.Passage -> {
            prompt.sentences.forEach { sentence ->
                Column {
                    EnglishText(text = sentence.text)
                    if (prompt.showsTranslations) {
                        sentence.ko?.let {
                            KoreanProse(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            KoreanProse(text = prompt.question, style = MaterialTheme.typography.titleMedium)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(GilTheme.spacing.inlineGap)) {
        step.options.forEachIndexed { index, option ->
            val mark = when {
                step.correct == null -> OptionMark.IDLE
                index == step.correct -> OptionMark.CORRECT
                index == step.picked -> OptionMark.PICKED_WRONG
                else -> OptionMark.OTHER
            }
            ChoiceOption(option = option, mark = mark, onClick = { onAction(LessonAction.OnOptionClick(index)) })
        }
    }
}

@Composable
private fun ColumnScope.TypedContent(
    step: StepUi.Typed,
    hasVoice: Boolean,
    answered: Boolean,
    onAction: (LessonAction) -> Unit
) {
    when (val prompt = step.prompt) {
        is TypedPrompt.Spell -> {
            if (hasVoice) {
                SoundButton(onClick = {
                    onAction(LessonAction.OnPlayClick)
                }, description = stringResource(R.string.lesson_play), large = true)
            }
            KoreanProse(text = prompt.ko, style = MaterialTheme.typography.titleLarge)
            IpaText(ipa = prompt.ipa, spokenWord = "")
        }

        TypedPrompt.Dictation -> if (hasVoice) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(GilTheme.spacing.itemGap)
            ) {
                SoundButton(onClick = {
                    onAction(LessonAction.OnPlayClick)
                }, description = stringResource(R.string.lesson_play), large = true)
                TextActionButton(text = stringResource(R.string.lesson_play_slowly), onClick = {
                    onAction(LessonAction.OnPlaySlowlyClick)
                })
            }
        }

        is TypedPrompt.Blank -> {
            EnglishText(text = prompt.text, size = EnglishTextSize.Focus)
            KoreanProse(text = prompt.ko, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    OutlinedTextField(
        value = step.text,
        onValueChange = { onAction(LessonAction.OnTextChange(it)) },
        modifier = Modifier.fillMaxWidth(),
        enabled = !answered,
        label = { Text(stringResource(R.string.lesson_answer_label)) },
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
        keyboardActions = KeyboardActions(onDone = { onAction(LessonAction.OnCheckClick) })
    )
}

private val EnglishLocale = LocaleList("en-US")
