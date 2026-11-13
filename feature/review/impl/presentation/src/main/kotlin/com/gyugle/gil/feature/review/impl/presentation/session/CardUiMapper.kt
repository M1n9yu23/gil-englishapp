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

import com.gyugle.gil.core.designsystem.component.BLANK
import com.gyugle.gil.feature.review.impl.domain.model.AnswerGrade
import com.gyugle.gil.feature.review.impl.domain.model.ReviewCard

internal fun ReviewCard.kindUi(hasVoice: Boolean): CardKindUi = when (this) {
    is ReviewCard.ChooseMeaning -> CardKindUi.CHOOSE_MEANING
    is ReviewCard.ChooseEnglish -> CardKindUi.CHOOSE_ENGLISH
    is ReviewCard.ListenChoose -> CardKindUi.LISTEN_CHOOSE
    is ReviewCard.ChooseLetter -> if (hasVoice) CardKindUi.CHOOSE_LETTER_BY_EAR else CardKindUi.CHOOSE_LETTER_BY_NAME
    is ReviewCard.FindSound -> CardKindUi.FIND_SOUND
    is ReviewCard.Spell -> if (speakable) CardKindUi.SPELL_BY_EAR else CardKindUi.SPELL_BY_MEANING
    is ReviewCard.FillBlank -> CardKindUi.FILL_BLANK
}

/** What the play button says, or null when the card has nothing to hear. */
internal fun ReviewCard.spoken(hasVoice: Boolean): String? = when (this) {
    is ReviewCard.ChooseMeaning -> english.takeIf { speakable }
    is ReviewCard.ListenChoose -> spoken
    is ReviewCard.ChooseLetter -> spoken.takeIf { hasVoice }
    is ReviewCard.Spell -> word.takeIf { speakable }
    is ReviewCard.ChooseEnglish, is ReviewCard.FindSound, is ReviewCard.FillBlank -> null
}

/** The card with the learner's [picked] option or typed [text]; a pick shows the right answer too. */
internal fun ReviewCard.toCardUi(hasVoice: Boolean, picked: Int?, text: String): CardUi = when (this) {
    is ReviewCard.ChooseMeaning -> choice(
        ChoicePromptUi.Meaning(english, ipa, speakable),
        options.map { AnswerUi(it, english = false) },
        picked
    )

    is ReviewCard.ChooseEnglish -> choice(
        ChoicePromptUi.English(meaningKo),
        options.map { AnswerUi(it, english = true) },
        picked
    )

    is ReviewCard.ListenChoose -> choice(
        ChoicePromptUi.Listen(revealed = spoken.takeIf { picked != null }),
        options.map { AnswerUi(it, english = true) },
        picked
    )

    is ReviewCard.ChooseLetter -> choice(
        ChoicePromptUi.Letter(nameIpa, showsIpa = !hasVoice || picked != null),
        options.map { AnswerUi(it, english = true) },
        picked
    )

    // The IPA would give the answer away, so it shows only once the card is answered.
    is ReviewCard.FindSound -> choice(
        ChoicePromptUi.Sound(symbol, spelling.takeIf { it.isNotBlank() }),
        options.map { AnswerUi(it.text, english = true, ipa = it.ipa.takeIf { picked != null }) },
        picked
    )

    is ReviewCard.Spell -> CardUi.Typed(TypedPromptUi.Spell(meaningKo, ipa, speakable), text)

    is ReviewCard.FillBlank -> CardUi.Typed(
        TypedPromptUi.Blank(sentence.replaceRange(blankStart, blankEnd, BLANK), translation),
        text
    )
}

private fun ReviewCard.ChoiceCard.choice(prompt: ChoicePromptUi, options: List<AnswerUi>, picked: Int?) =
    CardUi.Choice(prompt, options, picked = picked, correct = answer.takeIf { picked != null })

/**
 * The verdict, with the right English when a typed answer was not exact, and what the card did
 * not show before: the meaning of a word only heard, a sound's description.
 */
internal fun ReviewCard.feedback(grade: AnswerGrade): FeedbackUi {
    val ui = grade.toGradeUi()
    val exact = grade == AnswerGrade.CORRECT
    return when (this) {
        is ReviewCard.ListenChoose -> FeedbackUi(ui, note = meaningKo)
        is ReviewCard.FindSound -> FeedbackUi(ui, note = descriptionKo)
        is ReviewCard.Spell -> FeedbackUi(ui, answer = word.takeUnless { exact })
        is ReviewCard.FillBlank -> FeedbackUi(ui, answer = answer.takeUnless { exact }, note = meaningKo)
        is ReviewCard.ChooseMeaning, is ReviewCard.ChooseEnglish, is ReviewCard.ChooseLetter -> FeedbackUi(ui)
    }
}

private fun AnswerGrade.toGradeUi(): GradeUi = when (this) {
    AnswerGrade.CORRECT -> GradeUi.CORRECT
    AnswerGrade.CLOSE -> GradeUi.CLOSE
    AnswerGrade.WRONG -> GradeUi.WRONG
}
