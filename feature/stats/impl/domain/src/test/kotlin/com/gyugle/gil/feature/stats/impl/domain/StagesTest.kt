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
package com.gyugle.gil.feature.stats.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageLessons
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.stats.impl.domain.model.StageProgress
import org.junit.jupiter.api.Test

class StagesTest {
    private val s0 = StageTag(StageId("stage.s0"), 0, LocalizedText("소리", "Sounds"))
    private val s1 = StageTag(StageId("stage.s1"), 1, LocalizedText("글자", "Letters"))
    private val a1 = StageTag(StageId("stage.a1"), 2, LocalizedText("첫걸음", "First steps"))
    private val stages = listOf(
        StageLessons(s0, listOf(LessonId("lesson.s0-01.1"), LessonId("lesson.s0-01.2"))),
        StageLessons(s1, listOf(LessonId("lesson.s1-01.1"))),
        StageLessons(a1, emptyList())
    )

    @Test
    fun stageProgress_someDone_countsDoneLessonsPerStage() {
        val progress = stageProgress(stages, setOf(LessonId("lesson.s0-01.2")))

        assertThat(progress).isEqualTo(listOf(StageProgress(s0, 1, 2), StageProgress(s1, 0, 1)))
    }

    @Test
    fun stageProgress_lessonNoLongerInTheCourse_isNotCounted() {
        val progress = stageProgress(stages, setOf(LessonId("lesson.s0-01.1"), LessonId("lesson.retired.1")))

        assertThat(progress.map { it.completed }).isEqualTo(listOf(1, 0))
    }
}
