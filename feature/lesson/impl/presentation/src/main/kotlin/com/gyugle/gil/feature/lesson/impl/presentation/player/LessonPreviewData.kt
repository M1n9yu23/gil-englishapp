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

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.presentation.UiText
import com.gyugle.gil.feature.lesson.impl.presentation.R

/** Lesson states for previews and screenshot tests, written like real content. */
internal object LessonPreviewData {
    val title = LocalizedTextUi(ko = "f와 v", en = "f and v")

    fun playing(
        step: StepUi,
        feedback: FeedbackUi? = null,
        stepNumber: Int = 3,
        stepCount: Int = 9,
        isAgain: Boolean = false,
        hasVoice: Boolean = true,
        showsNoVoiceNotice: Boolean = false,
        canCheck: Boolean = false
    ) = LessonState(
        title = title,
        phase = LessonPhase.Playing(
            stepNumber,
            stepCount,
            isAgain,
            hasVoice,
            showsNoVoiceNotice,
            step,
            feedback,
            canCheck
        )
    )

    val explain = StepUi.Explain(
        paragraphs = listOf(
            "`fan`과 `van`을 들어 보세요. 둘 다 윗니를 아랫입술에 살짝 대고 내는 소리예요.",
            "`f`는 바람만 내보내고, `v`는 같은 자리에서 목을 울려요. 두 입술이 붙는 우리말 \"파\", \"바\"와 달라요."
        ),
        examples = listOf(SentenceUi("Fan.", "부채."), SentenceUi("Van.", "승합차."))
    )

    val listen = StepUi.Listen(SentenceUi("Five fish.", "물고기 다섯 마리."), isMeaningShown = false)

    val shadow = StepUi.Shadow(SentenceUi("Very good.", "아주 좋아요."), RecorderUi(MicrophoneUi.NOT_ASKED))

    val shadowRecorded = shadow.copy(recorder = RecorderUi(MicrophoneUi.GRANTED, hasRecording = true))

    val shadowScored = StepUi.Shadow(
        SentenceUi("Nice to meet you.", "만나서 반가워요."),
        RecorderUi(
            MicrophoneUi.GRANTED,
            hasRecording = true,
            score = ScoreUi.Heard(
                listOf(
                    MarkedWordUi("Nice", true),
                    MarkedWordUi("to", true),
                    MarkedWordUi("meet", false),
                    MarkedWordUi("you", true)
                )
            )
        )
    )

    val shadowNothingHeard =
        shadowRecorded.copy(
            recorder = RecorderUi(MicrophoneUi.GRANTED, hasRecording = true, score = ScoreUi.NothingHeard)
        )

    val shadowScoringNotice =
        shadow.copy(recorder = RecorderUi(MicrophoneUi.GRANTED, notice = ScoringNoticeUi.MODEL_NOT_INSTALLED))

    private val meaningOptions =
        listOf(OptionUi("승합차", english = false), OptionUi("부채", english = false), OptionUi("다섯", english = false))

    val chooseMeaning = StepUi.Choice(ChoicePrompt.English("fan"), meaningOptions, picked = null, correct = null)

    val chooseMeaningWrong = chooseMeaning.copy(picked = 0, correct = 1)

    val wrongFeedback = FeedbackUi(
        GradeUi.WRONG,
        why = "승합차는 `van`이에요. 들은 소리는 목이 울리지 않는 `f`였어요.",
        comesBack = true
    )

    val listenChooseAnswered = StepUi.Choice(
        ChoicePrompt.Audio("van"),
        listOf(OptionUi("f", english = true), OptionUi("v", english = true)),
        picked = 1,
        correct = 1
    )

    val chooseEnglish = StepUi.Choice(
        ChoicePrompt.Korean("모음 소리가 나머지 셋과 다른 단어"),
        listOf(OptionUi("bed", true), OptionUi("red", true), OptionUi("bead", true), OptionUi("ten", true)),
        picked = null,
        correct = null
    )

    val minimalPairAnswered = StepUi.Choice(
        ChoicePrompt.SoundPair,
        listOf(OptionUi("ship", true, ipa = "ʃɪp"), OptionUi("sheep", true, ipa = "ʃiːp")),
        picked = 1,
        correct = 1
    )

    val dialogue = StepUi.Choice(
        ChoicePrompt.Dialogue(
            lines = listOf(
                DialogueLineUi("Mina", "Hi, Jun. How are you?"),
                DialogueLineUi("Jun", "Fine, thanks. And you?")
            ),
            question = null
        ),
        listOf(OptionUi("I’m good, thanks.", true), OptionUi("You’re welcome.", true), OptionUi("Goodbye.", true)),
        picked = null,
        correct = null
    )

    val passage = StepUi.Choice(
        ChoicePrompt.Passage(
            sentences = listOf(SentenceUi("Hello.", "안녕하세요."), SentenceUi("Hi.", "안녕하세요.")),
            showsTranslations = false,
            question = "두 말은 언제 하나요?",
            questionNumber = 1,
            questionCount = 2
        ),
        listOf(OptionUi("고마울 때", false), OptionUi("사람을 만났을 때", false), OptionUi("잠자리에 들 때", false)),
        picked = null,
        correct = null
    )

    val fillBlankTyped = StepUi.Typed(TypedPrompt.Blank("P, p, Q, ___.", "피, 피, 큐, 큐(소문자)."), text = "q")

    val arrange = StepUi.Arrange(
        ko = "제 이름은 미나예요.",
        bank = listOf(TileUi("is"), TileUi("Mina."), TileUi("My"), TileUi("name"), TileUi("am")),
        chosen = listOf(2, 3)
    )

    val spell = StepUi.Typed(TypedPrompt.Spell("다섯", "faɪv"), text = "fiev")

    val dictation = StepUi.Typed(TypedPrompt.Dictation, text = "Red lip")

    val dictationFeedback = FeedbackUi(
        GradeUi.WRONG,
        answer = "Red leg.",
        comesBack = true,
        typed = listOf(MarkedWordUi("Red", true), MarkedWordUi("lip", false)),
        translation = "빨간 다리."
    )

    val result = LessonState(
        title = title,
        phase = LessonPhase.Finished(
            ResultUi(
                minutes = 12,
                goal = LocalizedTextUi(ko = "f와 v를 듣고 구별할 수 있어요", en = "Tell f from v by ear"),
                items = listOf(
                    ResultItemUi("f, ph", "f", null, comesBackSoon = false),
                    ResultItemUi("v", "v", null, comesBackSoon = true),
                    ResultItemUi("five", "faɪv", "다섯", comesBackSoon = false)
                ),
                hasNext = true,
                isSaving = false,
                saveFailed = false
            )
        )
    )

    val failed = LessonState(
        phase = LessonPhase.Failed(
            LessonErrorUi(
                title = UiText.StringResource(R.string.lesson_error_unavailable_title),
                body = UiText.StringResource(R.string.lesson_error_unavailable_body),
                canRetry = true
            )
        )
    )

    val askResume = LessonState(title = title, phase = LessonPhase.AskResume(step = 4, count = 9))
}

@PreviewLightDark
@Composable
private fun LessonScreenChoicePreview() {
    GilTheme {
        LessonScreen(
            state = LessonPreviewData.playing(LessonPreviewData.chooseMeaningWrong, LessonPreviewData.wrongFeedback),
            onAction = {}
        )
    }
}

@Preview
@Composable
private fun LessonScreenArrangePreview() {
    GilTheme {
        LessonScreen(state = LessonPreviewData.playing(LessonPreviewData.arrange, canCheck = true), onAction = {})
    }
}

@PreviewLightDark
@Composable
private fun LessonScreenResultPreview() {
    GilTheme {
        LessonScreen(state = LessonPreviewData.result, onAction = {})
    }
}
