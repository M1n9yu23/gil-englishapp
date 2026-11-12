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
package com.gyugle.gil.feature.review.impl.domain

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeCourseObserver
import com.gyugle.gil.feature.curriculum.api.Lesson
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageLessons
import com.gyugle.gil.feature.curriculum.api.StageTag
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StageTestTest {
    private fun item(n: Int) = LessonItem(SenseId("sense.w$n.n.x"), "w$n", null, "뜻 $n")

    private fun lesson(id: String, vararg items: LessonItem) =
        Lesson(LessonId(id), LocalizedText(id, id), LocalizedText("", ""), emptyList(), items.toList(), next = null)

    private fun stage(id: String, position: Int) = StageTag(StageId(id), position, LocalizedText(id, id))

    @Test
    fun allowedWrong_fullTest_threeOfTwenty() {
        assertThat(allowedWrong(20)).isEqualTo(3)
        assertThat(allowedWrong(10)).isEqualTo(1)
        assertThat(allowedWrong(6)).isEqualTo(0)
    }

    @Test
    fun pickTestItems_manyItems_oneFromEachStretch() {
        val items = (0 until 100).map(::item)

        val picked = pickTestItems(items, count = 20, random = Random(7))

        assertThat(picked).hasSize(20)
        // Each stretch of five holds exactly one picked item.
        val stretches = picked.map { items.indexOf(it) / 5 }
        assertThat(stretches.sorted()).isEqualTo((0 until 20).toList())
    }

    @Test
    fun pickTestItems_fewerItemsThanQuestions_takesEveryOne() {
        val items = (0 until 12).map(::item)

        assertThat(
            pickTestItems(items, count = 20, random = Random(1))
        ).containsExactlyInAnyOrder(*items.toTypedArray())
    }

    @Test
    fun plan_laterStage_skipsEveryLessonUpToItAndAsksAboutItsOwnItems() = runTest {
        val course = FakeCourseObserver(
            lesson("lesson.s0-01.1"),
            lesson("lesson.s1-01.1", item(1), item(2)),
            lesson("lesson.a1-01.1", item(3)),
            lesson("lesson.a1-01.2", item(4))
        ).apply {
            stages = listOf(
                StageLessons(stage("stage.s0", 0), listOf(LessonId("lesson.s0-01.1"))),
                StageLessons(stage("stage.s1", 1), listOf(LessonId("lesson.s1-01.1"))),
                StageLessons(stage("stage.a1", 2), listOf(LessonId("lesson.a1-01.1"), LessonId("lesson.a1-01.2"))),
                StageLessons(stage("stage.a2", 3), emptyList())
            )
        }

        val plan = (StageTestPlanner(course).plan(StageId("stage.s1")) as Outcome.Success).data

        assertThat(plan.stage).isEqualTo(stage("stage.s1", 1))
        assertThat(plan.lessons).containsExactly(LessonId("lesson.s0-01.1"), LessonId("lesson.s1-01.1"))
        assertThat(plan.items).containsExactly(item(1), item(2))
        assertThat(plan.questionCount).isEqualTo(2)
    }

    @Test
    fun plan_unknownStage_notFound() = runTest {
        val course = FakeCourseObserver(lesson("lesson.s1-01.1", item(1)))

        assertThat(StageTestPlanner(course).plan(StageId("stage.b2")))
            .isEqualTo(Outcome.Failure(ContentError.NOT_FOUND))
    }

    @Test
    fun plan_contentUnavailable_fails() = runTest {
        val course = FakeCourseObserver(lesson("lesson.s1-01.1", item(1))).apply { error = ContentError.UNAVAILABLE }

        assertThat(StageTestPlanner(course).plan(StageId("stage.s1")))
            .isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
    }
}
