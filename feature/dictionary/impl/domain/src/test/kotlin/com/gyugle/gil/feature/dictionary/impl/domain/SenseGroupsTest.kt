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
package com.gyugle.gil.feature.dictionary.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.dictionary.impl.domain.model.SenseGroups
import org.junit.jupiter.api.Test

class SenseGroupsTest {
    private val move = sense("run.move", stage = 2)
    private val manage = sense("run.manage", stage = 4)
    private val flow = sense("run.flow", stage = 4)
    private val senses = listOf(move, manage, flow)

    @Test
    fun groupSenses_someLearned_showsThoseNowAndTheRestLater() {
        val groups = groupSenses(senses, learned = setOf(manage.id))

        assertThat(groups).isEqualTo(SenseGroups(now = listOf(manage), later = listOf(move, flow), learnedAny = true))
    }

    @Test
    fun groupSenses_noneLearned_showsTheEarliestStageNow() {
        val groups = groupSenses(listOf(manage, move, flow), learned = emptySet())

        assertThat(groups).isEqualTo(SenseGroups(now = listOf(move), later = listOf(manage, flow), learnedAny = false))
    }

    @Test
    fun groupSenses_noneLearnedAndSeveralInTheEarliestStage_showsThemAll() {
        val groups = groupSenses(listOf(manage, flow), learned = emptySet())

        assertThat(groups).isEqualTo(SenseGroups(now = listOf(manage, flow), later = emptyList(), learnedAny = false))
    }

    @Test
    fun groupSenses_focus_countsAsKnownNow() {
        val groups = groupSenses(senses, learned = setOf(move.id), focus = flow.id)

        assertThat(groups).isEqualTo(SenseGroups(now = listOf(move, flow), later = listOf(manage), learnedAny = true))
    }

    @Test
    fun groupSenses_focusOnAnUnlearnedWord_addsItToTheFirstStageSenses() {
        val groups = groupSenses(senses, learned = emptySet(), focus = flow.id)

        assertThat(groups).isEqualTo(SenseGroups(now = listOf(move, flow), later = listOf(manage), learnedAny = false))
    }

    @Test
    fun groupSenses_learnedIdOfAnotherWord_isIgnored() {
        val groups = groupSenses(senses, learned = setOf(SenseId("sense.walk.go")))

        assertThat(groups.learnedAny).isEqualTo(false)
        assertThat(groups.now).isEqualTo(listOf(move))
    }

    @Test
    fun groupSenses_noSenses_isEmpty() {
        assertThat(groupSenses(emptyList(), emptySet()))
            .isEqualTo(SenseGroups(now = emptyList(), later = emptyList(), learnedAny = false))
    }
}
