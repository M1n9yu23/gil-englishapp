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

/** States for previews and screenshot tests. */
internal object ReviewPreviewData {
    private val meaning = ReviewPhase.Asking(
        number = 3,
        count = 12,
        kind = CardKindUi.CHOOSE_MEANING,
        card = CardUi.Choice(
            prompt = ChoicePromptUi.Meaning(english = "teacher", ipa = "ˈtiːtʃər", speakable = true),
            options = listOf("학생", "선생님", "책", "가방").map { AnswerUi(it, english = false) }
        ),
        hasVoice = true
    )

    val meaningIdle = ReviewState(meaning)

    val meaningAnswered = ReviewState(
        meaning.copy(
            card = (meaning.card as CardUi.Choice).copy(picked = 0, correct = 1),
            feedback = FeedbackUi(GradeUi.WRONG)
        )
    )

    val englishIdle = ReviewState(
        ReviewPhase.Asking(
            number = 5,
            count = 12,
            kind = CardKindUi.CHOOSE_ENGLISH,
            card = CardUi.Choice(
                prompt = ChoicePromptUi.English("고마워요"),
                options = listOf("Thank you.", "You're welcome.", "Goodbye.").map { AnswerUi(it, english = true) }
            ),
            hasVoice = true
        )
    )

    val listenAnswered = ReviewState(
        ReviewPhase.Asking(
            number = 6,
            count = 12,
            kind = CardKindUi.LISTEN_CHOOSE,
            card = CardUi.Choice(
                prompt = ChoicePromptUi.Listen(revealed = "thirteen"),
                options = listOf("thirty", "thirteen", "three").map { AnswerUi(it, english = true) },
                picked = 1,
                correct = 1
            ),
            hasVoice = true,
            feedback = FeedbackUi(GradeUi.CORRECT, note = "13, 열셋")
        )
    )

    val letterNoVoice = ReviewState(
        ReviewPhase.Asking(
            number = 1,
            count = 4,
            kind = CardKindUi.CHOOSE_LETTER_BY_NAME,
            card = CardUi.Choice(
                prompt = ChoicePromptUi.Letter(nameIpa = "dʒiː", showsIpa = true),
                options = listOf("G g", "J j", "Z z", "E e").map { AnswerUi(it, english = true) }
            ),
            hasVoice = false
        )
    )

    val soundAnswered = ReviewState(
        ReviewPhase.Asking(
            number = 2,
            count = 4,
            kind = CardKindUi.FIND_SOUND,
            card = CardUi.Choice(
                prompt = ChoicePromptUi.Sound("θ"),
                options = listOf(
                    AnswerUi("this", english = true, ipa = "ðɪs"),
                    AnswerUi("think", english = true, ipa = "θɪŋk"),
                    AnswerUi("ship", english = true, ipa = "ʃɪp")
                ),
                picked = 0,
                correct = 1
            ),
            hasVoice = true,
            feedback = FeedbackUi(
                GradeUi.WRONG,
                note = "혀끝을 윗니와 아랫니 사이에 살짝 물고 바람만 내보내요."
            )
        )
    )

    val spellTyping = ReviewState(
        ReviewPhase.Asking(
            number = 8,
            count = 12,
            kind = CardKindUi.SPELL_BY_EAR,
            card = CardUi.Typed(TypedPromptUi.Spell("선생님", "ˈtiːtʃər", speakable = true), text = "teacher"),
            hasVoice = true
        )
    )

    val blankClose = ReviewState(
        ReviewPhase.Asking(
            number = 9,
            count = 12,
            kind = CardKindUi.FILL_BLANK,
            card = CardUi.Typed(TypedPromptUi.Blank("Two, $BLANK, four.", "둘, 셋, 넷."), text = "thre"),
            hasVoice = true,
            feedback = FeedbackUi(GradeUi.CLOSE, answer = "three", note = "3, 셋")
        )
    )

    val finished = ReviewState(
        ReviewPhase.Finished(
            ResultUi(
                answered = 12,
                right = 10,
                again = listOf(
                    ResultItemUi("teacher", "선생님", detailIsIpa = false),
                    ResultItemUi("th", "θ", detailIsIpa = true)
                ),
                remaining = 8,
                nextInDays = null
            )
        )
    )

    val finishedAllDone = ReviewState(
        ReviewPhase.Finished(
            ResultUi(answered = 6, right = 6, again = emptyList(), remaining = 0, nextInDays = 1)
        )
    )

    val empty = ReviewState(ReviewPhase.Empty(nextInDays = 3))

    val emptyNoCards = ReviewState(ReviewPhase.Empty(nextInDays = null))

    val failed = ReviewState(ReviewPhase.Failed)
}
