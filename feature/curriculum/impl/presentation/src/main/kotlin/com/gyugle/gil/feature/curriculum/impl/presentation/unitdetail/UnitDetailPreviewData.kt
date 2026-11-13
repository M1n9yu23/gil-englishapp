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
package com.gyugle.gil.feature.curriculum.impl.presentation.unitdetail

import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.impl.presentation.LocalizedTextUi

/** A sample unit for previews and screenshot tests. */
internal object UnitDetailPreviewData {
    // The first lesson is done.
    private fun lesson(number: Int, titleKo: String, titleEn: String, goalKo: String, goalEn: String) = LessonRowUi(
        id = LessonId("lesson.s1-01.$number"),
        number = number,
        title = LocalizedTextUi(ko = titleKo, en = titleEn),
        goal = LocalizedTextUi(ko = goalKo, en = goalEn),
        done = number == 1
    )

    val unit = UnitDetailUi(
        title = LocalizedTextUi(ko = "알파벳 읽기", en = "Reading the alphabet"),
        stageTitle = LocalizedTextUi(ko = "소리와 글자", en = "Sounds and letters"),
        lessons = listOf(
            lesson(1, "A부터 G까지", "A to G", "글자 일곱 개의 이름과 소리를 알아요", "Know the names and sounds of seven letters"),
            lesson(2, "H부터 N까지", "H to N", "글자 일곱 개를 더 읽어요", "Read seven more letters"),
            lesson(3, "O부터 U까지", "O to U", "모음 글자를 알아봐요", "Spot the vowel letters"),
            lesson(4, "V부터 Z까지", "V to Z", "알파벳 26자를 모두 읽어요", "Read all 26 letters")
        )
    )
}
