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

import com.gyugle.gil.feature.dictionary.impl.presentation.R
import com.gyugle.gil.feature.dictionary.impl.presentation.component.LocalizedTextUi

/** Pages for previews and screenshot tests. */
internal object EntryPreviewData {
    val word = EntryUi.Word(
        id = "word.run",
        text = "run",
        ipa = "rʌn",
        hangul = "런",
        learnedAny = true,
        now = listOf(
            SenseUi(
                id = "sense.run.v.move-fast",
                partOfSpeech = R.string.dictionary_pos_verb,
                meaningKo = "달리다, 뛰다",
                definitionEn = null,
                ipa = null,
                examples = listOf(
                    ExampleUi(1, "I run every morning.", "나는 매일 아침 달려요."),
                    ExampleUi(2, "Don’t run in the hall.", "복도에서 뛰지 마세요.")
                ),
                saved = true
            )
        ),
        later = listOf(
            LaterSenseUi(
                "sense.run.v.manage",
                R.string.dictionary_pos_verb,
                "운영하다",
                LocalizedTextUi("자립", "On your own")
            ),
            LaterSenseUi("sense.run.n.jog", R.string.dictionary_pos_noun, "달리기", LocalizedTextUi("일상", "Everyday life"))
        ),
        collocations = listOf(PhraseUi("run fast", "빨리 달리다"), PhraseUi("go for a run", "달리러 가다")),
        family = listOf(FamilyWordUi("runner", "word.runner"), FamilyWordUi("running", null)),
        notes = listOf(
            NoteUi(R.string.dictionary_note_mistake, "과거형은 `runned`가 아니라 `ran`이에요."),
            NoteUi(R.string.dictionary_note_konglish, "헬스장의 ‘런닝머신’은 영어로 `treadmill`이에요.")
        )
    )

    /** A word the learner has not met: its first-stage meaning, nothing saved. */
    val newWord = word.copy(
        learnedAny = false,
        hangul = null,
        now = word.now.map { it.copy(saved = false) },
        notes = emptyList()
    )

    val pattern = EntryUi.Pattern(
        id = "pattern.thank-you",
        text = "Thank you.",
        meaningKo = "고마워요",
        noteKo = "`Thanks.`보다 조금 더 정중해요.",
        examples = listOf(ExampleUi(3, "Thank you, Sam.", "고마워요, 샘.")),
        saved = false
    )

    val sound = EntryUi.Sound(
        id = "sound.f",
        symbol = "f",
        spelling = "f, ph",
        descriptionKo = "윗니를 아랫입술에 가볍게 대고 바람을 내보내요. 한국어 ‘ㅍ’과 달리 입술이 닫히지 않아요.",
        examples = listOf(SoundWordUi("fan", "fæn"), SoundWordUi("phone", "foʊn"), SoundWordUi("coffee", "ˈkɔːfi")),
        saved = true
    )
}
