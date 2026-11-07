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
package com.gyugle.gil.feature.curriculum.impl.data.mapper

import com.gyugle.gil.feature.curriculum.api.Choice
import com.gyugle.gil.feature.curriculum.api.DialogueLine
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.PairWord
import com.gyugle.gil.feature.curriculum.api.PassageQuestion
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.impl.data.dto.LineDto
import com.gyugle.gil.feature.curriculum.impl.data.dto.OptionDto
import com.gyugle.gil.feature.curriculum.impl.data.dto.PairWordDto
import com.gyugle.gil.feature.curriculum.impl.data.dto.QuestionDto
import com.gyugle.gil.feature.curriculum.impl.data.dto.SentenceRefDto
import com.gyugle.gil.feature.curriculum.impl.data.dto.StepPayloadDto

fun StepPayloadDto.toLessonStep(): LessonStep = when (this) {
    is StepPayloadDto.Explain -> LessonStep.Explain(paragraphs, examples.map { it.toSentence() })

    is StepPayloadDto.Listen -> LessonStep.Listen(sentence.toSentence())

    is StepPayloadDto.Shadow -> LessonStep.Shadow(sentence.toSentence())

    is StepPayloadDto.ChooseMeaning ->
        LessonStep.ChooseMeaning(prompt.toSentence(), options.map { it.toChoice() }, item?.toStudyItemId())

    is StepPayloadDto.ListenChoose ->
        LessonStep.ListenChoose(audio.toSentence(), options.map { it.toChoice() }, item?.toStudyItemId())

    is StepPayloadDto.ChooseEnglish ->
        LessonStep.ChooseEnglish(ko, options.map { it.toChoice() }, item?.toStudyItemId())

    is StepPayloadDto.Arrange -> LessonStep.Arrange(
        ko = ko,
        answer = answer.toSentence(),
        tiles = tiles,
        accepted = accepted,
        extra = extra,
        item = item?.toStudyItemId()
    )

    is StepPayloadDto.FillBlank -> LessonStep.FillBlank(
        text = text,
        ko = ko,
        answers = answers,
        options = options.map { it.toChoice() },
        sentence = sentence.toSentence(),
        item = item?.toStudyItemId()
    )

    is StepPayloadDto.Spell -> LessonStep.Spell(answer = answer, ko = ko, ipa = ipa, item = item.toStudyItemId())

    is StepPayloadDto.Dictation -> LessonStep.Dictation(sentence.toSentence(), item?.toStudyItemId())

    is StepPayloadDto.MinimalPair ->
        LessonStep.MinimalPair(words.map { it.toPairWord() }, answer, why, item?.toStudyItemId())

    is StepPayloadDto.ReadPassage ->
        LessonStep.ReadPassage(sentences.map { it.toSentence() }, questions.map { it.toPassageQuestion() })

    is StepPayloadDto.Dialogue ->
        LessonStep.Dialogue(
            lines.map {
                it.toDialogueLine()
            },
            question,
            options.map { it.toChoice() },
            item?.toStudyItemId()
        )
}

/** The compiler only writes study item IDs here, so anything else means a broken file. */
fun String.toStudyItemId(): StudyItemId = checkNotNull(StudyItemId.parse(this)) { "Not a study item: $this" }

private fun SentenceRefDto.toSentence() = Sentence(id = id, text = text, ko = ko)

private fun OptionDto.toChoice() = Choice(text = text, english = english, correct = correct, why = why)

private fun PairWordDto.toPairWord() = PairWord(text = text, ipa = ipa)

private fun QuestionDto.toPassageQuestion() = PassageQuestion(ko = ko, options = options.map { it.toChoice() })

private fun LineDto.toDialogueLine() = DialogueLine(speaker = speaker, sentence = sentence.toSentence())
