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
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.dictionary.impl.domain.model.RankedHit
import org.junit.jupiter.api.Test

class SearchRankingTest {
    private val run = hit("run", "run.move", "run.manage")
    private val runner = hit("runner", "runner.person")
    private val rung = hit("rung", "rung.step")

    @Test
    fun learnedFirst_learnedHitsMoveUp_keepingTheContentOrderInEachGroup() {
        val ranked = listOf(
            run,
            runner,
            rung
        ).learnedFirst(setOf(SenseId("sense.rung.step"), SenseId("sense.runner.person")))

        assertThat(ranked).containsExactly(
            RankedHit(runner, learned = true),
            RankedHit(rung, learned = true),
            RankedHit(run, learned = false)
        )
    }

    @Test
    fun learnedFirst_oneLearnedMeaning_makesTheWordLearned() {
        val ranked = listOf(runner, run).learnedFirst(setOf(SenseId("sense.run.manage")))

        assertThat(ranked).containsExactly(RankedHit(run, learned = true), RankedHit(runner, learned = false))
    }

    @Test
    fun learnedFirst_nothingLearned_keepsTheOrder() {
        assertThat(listOf(run, runner).learnedFirst(emptySet()))
            .containsExactly(RankedHit(run, learned = false), RankedHit(runner, learned = false))
        assertThat(emptyList<com.gyugle.gil.feature.curriculum.api.SearchHit>().learnedFirst(emptySet())).isEmpty()
    }
}
