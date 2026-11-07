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

import com.gyugle.gil.feature.curriculum.api.Choice
import com.gyugle.gil.feature.curriculum.api.LessonStep
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.lesson.impl.domain.checkDictation
import com.gyugle.gil.feature.lesson.impl.domain.gradeArrange
import com.gyugle.gil.feature.lesson.impl.domain.gradeFillBlank
import com.gyugle.gil.feature.lesson.impl.domain.gradeSpelling
import com.gyugle.gil.feature.lesson.impl.domain.model.AnswerGrade
import kotlin.random.Random
internal fun LessonStep.toStepUi(
    input: StepInput,
    layout: StepLayout,
    hasVoice: Boolean,
    recorder: RecorderUi
): StepUi = when (this) {
    is LessonStep.Explain -> StepUi.Explain(paragraphs, examples.map { it.toSentenceUi() })

    is LessonStep.Listen -> StepUi.Listen(sentence.toSentenceUi(), input.isMeaningShown)

    is LessonStep.Shadow -> StepUi.Shadow(sentence.toSentenceUi(), recorder)

    is LessonStep.ChooseMeaning -> choice(ChoicePrompt.English(prompt.text), options, layout, input)

    is LessonStep.ListenChoose -> choice(
        ChoicePrompt.Audio(audio.text.takeIf { !hasVoice || input.feedback != null }),
        options,
        layout,
        input
    )

    is LessonStep.ChooseEnglish -> choice(ChoicePrompt.Korean(ko), options, layout, input)

    is LessonStep.Dialogue -> choice(
        ChoicePrompt.Dialogue(lines.map { DialogueLineUi(it.speaker, it.sentence.text) }, question),
        options,
        layout,
        input
    )

    is LessonStep.FillBlank ->
        if (options.isEmpty()) {
            StepUi.Typed(TypedPrompt.Blank(text, ko), input.text)
        } else {
            choice(ChoicePrompt.Blank(text, ko), options, layout, input)
        }

    is LessonStep.ReadPassage -> {
        val question = questions[input.question]
        choice(
            ChoicePrompt.Passage(
                sentences = sentences.map { it.toSentenceUi() },
                showsTranslations = input.grade != null,
                question = question.ko,
                questionNumber = input.question + 1,
                questionCount = questions.size
            ),
            question.options,
            layout,
            input,
            question = input.question
        )
    }

    is LessonStep.MinimalPair -> {
        val order = layout.order(words.size)
        StepUi.Choice(
            prompt = ChoicePrompt.SoundPair,
            options = order.map { OptionUi(words[it].text, english = true, ipa = words[it].ipa) },
            picked = input.picked,
            correct = order.indexOf(answer).takeIf { input.feedback != null }
        )
    }

    is LessonStep.Arrange -> StepUi.Arrange(ko, layout.bank.map(::TileUi), input.chosen)

    is LessonStep.Spell -> StepUi.Typed(TypedPrompt.Spell(ko, ipa), input.text)

    is LessonStep.Dictation -> StepUi.Typed(TypedPrompt.Dictation, input.text)
}

private fun choice(
    prompt: ChoicePrompt,
    choices: List<Choice>,
    layout: StepLayout,
    input: StepInput,
    question: Int = 0
): StepUi.Choice {
    val order = layout.order(choices.size, question)
    return StepUi.Choice(
        prompt = prompt,
        options = order.map { OptionUi(choices[it].text, choices[it].english) },
        picked = input.picked,
        correct = order.indexOfFirst { choices[it].correct }.takeIf { input.feedback != null }
    )
}

/** Whether the answer so far can be checked: something picked, typed or placed. */
internal fun LessonStep.canCheck(input: StepInput): Boolean = input.feedback == null &&
    when (this) {
        is LessonStep.Arrange -> input.chosen.isNotEmpty()
        is LessonStep.Spell, is LessonStep.Dictation -> input.text.isNotBlank()
        is LessonStep.FillBlank -> options.isEmpty() && input.text.isNotBlank()
        else -> false
    }

/** The outcome of checking [input] on a step that is typed or built from tiles. */
internal fun LessonStep.checkTyped(
    input: StepInput,
    layout: StepLayout,
    firstTry: Boolean
): Pair<AnswerGrade, FeedbackUi>? = when (this) {
    is LessonStep.Spell -> gradeSpelling(input.text, answer).let { grade ->
        grade to feedback(grade, firstTry, answer = answer.takeIf { grade != AnswerGrade.CORRECT })
    }

    is LessonStep.FillBlank -> if (options.isNotEmpty()) {
        null
    } else {
        gradeFillBlank(input.text, answers).let { grade ->
            grade to feedback(grade, firstTry, answer = sentence.text.takeIf { grade != AnswerGrade.CORRECT })
        }
    }

    is LessonStep.Dictation -> checkDictation(input.text, sentence.text).let { check ->
        check.grade to feedback(
            check.grade,
            firstTry,
            answer = sentence.text.takeIf { check.grade != AnswerGrade.CORRECT },
            typed = check.typed.map { MarkedWordUi(it.text, it.matched) }.takeIf {
                check.grade !=
                    AnswerGrade.CORRECT
            },
            translation = sentence.ko
        )
    }

    is LessonStep.Arrange -> gradeArrange(input.chosen.map { layout.bank[it] }, this).let { grade ->
        grade to feedback(grade, firstTry, answer = answer.text.takeIf { grade != AnswerGrade.CORRECT })
    }

    else -> null
}

/**
 * The outcome of picking [displayIndex] on a choice step. A passage question only grades the
 * whole step on its last question; before that the grade is null.
 */
internal fun LessonStep.checkPick(
    displayIndex: Int,
    input: StepInput,
    layout: StepLayout,
    firstTry: Boolean
): Pair<AnswerGrade?, FeedbackUi>? {
    fun pick(choices: List<Choice>, question: Int = 0): Choice? =
        layout.order(choices.size, question).getOrNull(displayIndex)?.let(choices::getOrNull)
    return when (this) {
        is LessonStep.MinimalPair -> {
            val index = layout.order(words.size).getOrNull(displayIndex) ?: return null
            val grade = if (index == answer) AnswerGrade.CORRECT else AnswerGrade.WRONG
            grade to feedback(grade, firstTry, why = why)
        }

        is LessonStep.ReadPassage -> {
            val choice = pick(questions[input.question].options, input.question) ?: return null
            val missed = input.missedQuestion || !choice.correct
            val last = input.question == questions.lastIndex
            val stepGrade = if (missed) AnswerGrade.WRONG else AnswerGrade.CORRECT
            // The passage comes back as a whole, so the note goes with its last question.
            (stepGrade.takeIf { last }) to feedback(
                if (choice.correct) AnswerGrade.CORRECT else AnswerGrade.WRONG,
                firstTry = false,
                why = choice.why.takeIf { !choice.correct }
            ).copy(comesBack = firstTry && last && missed)
        }

        // The right option is the meaning, so a translation would only repeat it.
        is LessonStep.ChooseMeaning -> choiceOutcome(pick(options), firstTry)

        is LessonStep.ListenChoose -> choiceOutcome(pick(options), firstTry, translation = audio.ko)

        is LessonStep.ChooseEnglish -> choiceOutcome(pick(options), firstTry)

        is LessonStep.Dialogue -> choiceOutcome(pick(options), firstTry)

        is LessonStep.FillBlank -> choiceOutcome(pick(options), firstTry, translation = null)

        else -> null
    }
}

private fun choiceOutcome(
    choice: Choice?,
    firstTry: Boolean,
    translation: String? = null
): Pair<AnswerGrade, FeedbackUi>? {
    choice ?: return null
    val grade = if (choice.correct) AnswerGrade.CORRECT else AnswerGrade.WRONG
    return grade to feedback(grade, firstTry, why = choice.why.takeIf { !choice.correct }, translation = translation)
}

private fun feedback(
    grade: AnswerGrade,
    firstTry: Boolean,
    answer: String? = null,
    why: String? = null,
    typed: List<MarkedWordUi>? = null,
    translation: String? = null
) = FeedbackUi(
    grade = when (grade) {
        AnswerGrade.CORRECT -> GradeUi.CORRECT
        AnswerGrade.CLOSE -> GradeUi.CLOSE
        AnswerGrade.WRONG -> GradeUi.WRONG
    },
    answer = answer,
    why = why,
    comesBack = grade == AnswerGrade.WRONG && firstTry,
    typed = typed,
    translation = translation
)

private fun Sentence.toSentenceUi() = SentenceUi(text, ko)
