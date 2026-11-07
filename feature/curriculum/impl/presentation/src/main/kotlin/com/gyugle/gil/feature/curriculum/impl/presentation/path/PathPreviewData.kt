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
package com.gyugle.gil.feature.curriculum.impl.presentation.path

import com.gyugle.gil.feature.curriculum.api.UnitId
import com.gyugle.gil.feature.curriculum.impl.presentation.LocalizedTextUi

/** Sample stages for previews and screenshot tests: one passed, the current one, and two ahead. */
internal object PathPreviewData {
    private fun unit(id: String, ko: String, en: String, lessons: Int) =
        UnitRowUi(id = UnitId(id), title = LocalizedTextUi(ko = ko, en = en), lessonCount = lessons)

    val stages = listOf(
        StageUi(
            id = "stage.s0",
            title = LocalizedTextUi(ko = "출발", en = "Getting started"),
            canDo = listOf(
                LocalizedTextUi(ko = "영어로 인사하고 이름을 말할 수 있어요", en = "Greet someone and say your name in English")
            ),
            units = listOf(unit("unit.s0-01", "인사와 이름", "Hello and names", 4)),
            place = StagePlace.PASSED
        ),
        StageUi(
            id = "stage.s1",
            title = LocalizedTextUi(ko = "소리와 글자", en = "Sounds and letters"),
            canDo = listOf(
                LocalizedTextUi(ko = "알파벳 26자를 읽을 수 있어요", en = "Read all 26 letters"),
                LocalizedTextUi(ko = "짧은 모음 다섯 개를 듣고 구별할 수 있어요", en = "Tell the five short vowels apart by ear")
            ),
            units = listOf(
                unit("unit.s1-01", "알파벳 읽기", "Reading the alphabet", 5),
                unit("unit.s1-02", "짧은 모음", "Short vowels", 1)
            ),
            place = StagePlace.CURRENT
        ),
        StageUi(
            id = "stage.a1",
            title = LocalizedTextUi(ko = "첫걸음", en = "First steps"),
            canDo = emptyList(),
            units = listOf(unit("unit.a1-01", "나와 가족", "Me and my family", 6)),
            place = StagePlace.AHEAD
        ),
        StageUi(
            id = "stage.a2",
            title = LocalizedTextUi(ko = "일상", en = "Everyday life"),
            canDo = emptyList(),
            units = emptyList(),
            place = StagePlace.AHEAD
        )
    )
}
