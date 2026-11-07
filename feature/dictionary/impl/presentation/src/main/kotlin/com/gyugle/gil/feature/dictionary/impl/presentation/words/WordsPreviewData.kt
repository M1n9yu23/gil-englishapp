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
package com.gyugle.gil.feature.dictionary.impl.presentation.words

import com.gyugle.gil.feature.dictionary.impl.presentation.component.ItemRowUi
import com.gyugle.gil.feature.dictionary.impl.presentation.component.LocalizedTextUi

/** Lists for previews and screenshot tests, in the shape the first stage gives. */
internal object WordsPreviewData {
    val saved = listOf(
        ItemRowUi("sense.hello.interj.greeting", "hello", "həˈloʊ", "안녕하세요(만날 때)"),
        ItemRowUi("pattern.thank-you", "Thank you.", null, "고마워요"),
        ItemRowUi("sense.seven.num.count", "seven", "ˈsɛvən", "7, 일곱"),
        ItemRowUi("sound.th-voiceless", "th", "θ", null, isSound = true)
    )

    val mistakes = listOf(
        ItemRowUi("sound.f", "f, ph", "f", null, mistakes = 3, isSound = true),
        ItemRowUi("sense.three.num.count", "three", "θriː", "3, 셋", mistakes = 2),
        ItemRowUi("sense.bye.interj.parting", "bye", "baɪ", "잘 가요(헤어질 때)", mistakes = 1)
    )

    val search = SearchUi.Results(
        learned = listOf(ItemRowUi("word.thank", "thank", "θæŋk", "고마워하다")),
        notLearned = listOf(
            ItemRowUi("word.thanks", "thanks", "θæŋks", "고마워요, 감사", stage = LocalizedTextUi("일상", "Everyday life")),
            ItemRowUi(
                "pattern.thank-you-very-much",
                "Thank you very much.",
                null,
                "정말 고마워요",
                stage = LocalizedTextUi("일상", "Everyday life")
            )
        )
    )
}
