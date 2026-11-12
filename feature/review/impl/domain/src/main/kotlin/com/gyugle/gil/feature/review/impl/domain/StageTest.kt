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

import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.CourseObserver
import com.gyugle.gil.feature.curriculum.api.LessonId
import com.gyugle.gil.feature.curriculum.api.LessonItem
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.review.api.stageTestQuestions
import javax.inject.Inject
import kotlin.random.Random

/** The share of a stage test's questions, in percent, that may be answered wrong. */
private const val WRONG_PERCENT = 15

/** How many wrong answers a test of [questions] questions allows and still passes: 3 of 20. */
fun allowedWrong(questions: Int): Int = questions * WRONG_PERCENT / 100

/**
 * What a test of [stage] is made of. [lessons] are the lessons passing it records as finished:
 * those of the stage and of every stage before it, in course order. [items] are the items the
 * stage's own lessons introduce, in course order, which the questions are about.
 */
data class StageTestPlan(val stage: StageTag, val lessons: List<LessonId>, val items: List<LessonItem>) {
    val questionCount: Int get() = stageTestQuestions(items.size)
}

/** Reads what a stage test needs from the course. */
class StageTestPlanner @Inject constructor(private val course: CourseObserver) {

    /** [ContentError.NOT_FOUND] when the course has no stage [stageId]. */
    suspend fun plan(stageId: StageId): Outcome<StageTestPlan, ContentError> {
        val stages = when (val read = course.getStageLessons()) {
            is Outcome.Failure -> return read
            is Outcome.Success -> read.data
        }
        val index = stages.indexOfFirst { it.stage.id == stageId }
        if (index < 0) return Outcome.Failure(ContentError.NOT_FOUND)
        val items = when (val read = course.getStageItems(stageId)) {
            is Outcome.Failure -> return read
            is Outcome.Success -> read.data
        }
        val lessons = stages.take(index + 1).flatMap { it.lessons }
        return Outcome.Success(StageTestPlan(stages[index].stage, lessons, items))
    }
}

/**
 * [count] of [items], which are in course order, spread over all of them: one from each of [count]
 * equal stretches, at a random place in it, so a test asks about the end of a stage as much as its
 * start. They come back in random order. Every item when there are no more than [count].
 */
fun pickTestItems(items: List<LessonItem>, count: Int, random: Random): List<LessonItem> {
    if (items.size <= count) return items.shuffled(random)
    val picked = (0 until count).map { stretch ->
        val start = stretch * items.size / count
        val end = (stretch + 1) * items.size / count
        items[random.nextInt(start, end)]
    }
    return picked.shuffled(random)
}
