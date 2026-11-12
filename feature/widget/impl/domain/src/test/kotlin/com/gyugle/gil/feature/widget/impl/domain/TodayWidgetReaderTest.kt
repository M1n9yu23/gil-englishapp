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
package com.gyugle.gil.feature.widget.impl.domain

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.gyugle.gil.core.domain.Outcome
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.api.FakeLexiconObserver
import com.gyugle.gil.feature.curriculum.api.LocalizedText
import com.gyugle.gil.feature.curriculum.api.PartOfSpeech
import com.gyugle.gil.feature.curriculum.api.PatternEntry
import com.gyugle.gil.feature.curriculum.api.PatternId
import com.gyugle.gil.feature.curriculum.api.SenseId
import com.gyugle.gil.feature.curriculum.api.Sentence
import com.gyugle.gil.feature.curriculum.api.StageId
import com.gyugle.gil.feature.curriculum.api.StageTag
import com.gyugle.gil.feature.curriculum.api.StudyItemId
import com.gyugle.gil.feature.curriculum.api.WordEntry
import com.gyugle.gil.feature.curriculum.api.WordId
import com.gyugle.gil.feature.curriculum.api.WordSense
import com.gyugle.gil.feature.progress.api.CardState
import com.gyugle.gil.feature.progress.api.FakeProgressObserver
import com.gyugle.gil.feature.progress.api.StudyDay
import com.gyugle.gil.feature.widget.impl.domain.model.DaySentence
import com.gyugle.gil.feature.widget.impl.domain.model.TodayWidgetContent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class TodayWidgetReaderTest {
    private val today = StudyDay(20_000)
    private val stage = StageTag(StageId("stage.a1"), 2, LocalizedText("첫걸음", "First steps"))
    private val run = SenseId("sense.run.v.move-fast")
    private val runExamples = listOf(Sentence(11, "I run every day.", "나는 매일 달려요."), Sentence(12, "Run!", null))
    private val runWord = WordEntry(
        id = WordId("word.run"),
        text = "run",
        ipa = "rʌn",
        hangul = null,
        senses = listOf(WordSense(run, PartOfSpeech.VERB, "달리다", null, null, stage, runExamples)),
        collocations = emptyList(),
        family = emptyList(),
        notes = emptyList()
    )
    private val howAreYou = PatternId("pattern.how-are-you")
    private val howAreYouEntry = PatternEntry(
        id = howAreYou,
        text = "How are you?",
        meaningKo = "어떻게 지내요?",
        noteKo = null,
        stage = stage,
        examples = listOf(Sentence(21, "How are you, Mina?", "미나, 어떻게 지내요?"))
    )
    private val progress = FakeProgressObserver()
    private val lexicon = FakeLexiconObserver(runWord, howAreYouEntry)
    private val reader = TodayWidgetReader(progress, lexicon)

    private fun card(id: StudyItemId, due: Int, lastReview: Int = -2, reviews: Int = 2) = CardState(
        id,
        stabilityDays = 3.0,
        dueDay = today + due,
        lastReviewDay = today + lastReview,
        reviewCount = reviews,
        lapseCount = 0
    )

    private fun Outcome<TodayWidgetContent, ContentError>.content() = (this as Outcome.Success).data

    @Test
    fun observe_noRecord_hasNothingDueAndNoSentence() = runTest {
        reader.observe(today).test {
            assertThat(awaitItem()).isEqualTo(Outcome.Success(TodayWidgetContent(dueCount = 0, sentence = null)))
        }
    }

    @Test
    fun observe_countsTheCardsDueTodayOrEarlier() = runTest {
        progress.cards.value = listOf(
            card(run, due = -1),
            card(howAreYou, due = 0),
            card(SenseId("sense.walk.v.move"), due = 1)
        )

        reader.observe(today).test {
            assertThat(awaitItem().content().dueCount).isEqualTo(2)
        }
    }

    @Test
    fun observe_aWordMetBefore_showsTheDaysExampleWithItsWordsPage() = runTest {
        progress.cards.value = listOf(card(run, due = 2))

        reader.observe(today).test {
            assertThat(awaitItem().content().sentence)
                .isEqualTo(DaySentence(runExamples[exampleIndex(today, 2)], WordId("word.run")))
        }
    }

    @Test
    fun observe_aPatternMetBefore_showsItsExampleWithThePatternsPage() = runTest {
        progress.cards.value = listOf(card(howAreYou, due = 2))

        reader.observe(today).test {
            assertThat(
                awaitItem().content().sentence
            ).isEqualTo(DaySentence(howAreYouEntry.examples.single(), howAreYou))
        }
    }

    @Test
    fun observe_onlyItemsFirstMetToday_hasNoSentenceYet() = runTest {
        progress.cards.value = listOf(card(run, due = 1, lastReview = 0, reviews = 1))

        reader.observe(today).test {
            assertThat(awaitItem().content().sentence).isNull()
        }
    }

    @Test
    fun observe_anItemTheDictionaryNoLongerHas_isSkipped() = runTest {
        progress.cards.value = listOf(card(SenseId("sense.gone.n.x"), due = 2), card(howAreYou, due = 2))

        reader.observe(today).test {
            assertThat(awaitItem().content().sentence?.entryId).isEqualTo(howAreYou)
        }
    }

    @Test
    fun observe_aCardReviewed_updatesTheCountAndKeepsTheSentence() = runTest {
        progress.cards.value = listOf(card(run, due = 0), card(howAreYou, due = 0))

        reader.observe(today).test {
            val before = awaitItem().content()
            progress.cards.value = listOf(card(run, due = 4, lastReview = 0, reviews = 3), card(howAreYou, due = 0))

            val after = awaitItem().content()
            assertThat(after.dueCount).isEqualTo(1)
            assertThat(after.sentence).isEqualTo(before.sentence)
        }
    }

    @Test
    fun observe_dictionaryUnreadable_fails() = runTest {
        progress.cards.value = listOf(card(run, due = 2))
        lexicon.error = ContentError.UNAVAILABLE

        reader.observe(today).test {
            assertThat(awaitItem()).isEqualTo(Outcome.Failure(ContentError.UNAVAILABLE))
        }
    }
}
