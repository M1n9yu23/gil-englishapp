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
package com.gyugle.gil.feature.lesson.impl.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.lesson.impl.domain.model.MarkedWord
import org.junit.jupiter.api.Test

class SpeechMatcherTest {

    @Test
    fun match_sameWords_everyWordMatched() {
        assertThat(SpeechMatcher.match("Good morning.", "good morning")).isEqualTo(
            listOf(MarkedWord("Good", true), MarkedWord("morning", true))
        )
    }

    @Test
    fun match_contractionHeardInFull_matched() {
        assertThat(SpeechMatcher.match("I’m fine.", "I am fine").all { it.matched }).isEqualTo(true)
        assertThat(SpeechMatcher.match("I am fine.", "I'm fine").all { it.matched }).isEqualTo(true)
    }

    @Test
    fun match_negativeContractions_matched() {
        assertThat(SpeechMatcher.match("I can’t swim.", "I cannot swim").all { it.matched }).isEqualTo(true)
        assertThat(SpeechMatcher.match("It won’t rain.", "it will not rain").all { it.matched }).isEqualTo(true)
        assertThat(SpeechMatcher.match("Don’t go.", "do not go").all { it.matched }).isEqualTo(true)
    }

    @Test
    fun match_itsHeardAsItIs_matched() {
        assertThat(SpeechMatcher.match("It’s red.", "it is red").all { it.matched }).isEqualTo(true)
    }

    @Test
    fun match_possessive_isNotOpenedUp() {
        assertThat(SpeechMatcher.canonicalWords("mina's")).isEqualTo(listOf("mina's"))
    }

    @Test
    fun match_numberHeardAsDigits_matched() {
        assertThat(SpeechMatcher.match("Ten.", "10")).isEqualTo(listOf(MarkedWord("Ten", true)))
        assertThat(SpeechMatcher.match("I’m twenty-one.", "I'm 21").all { it.matched }).isEqualTo(true)
        assertThat(SpeechMatcher.canonicalWords("105")).isEqualTo(listOf("one", "hundred", "five"))
        assertThat(SpeechMatcher.canonicalWords("0")).isEqualTo(listOf("zero"))
    }

    @Test
    fun match_skippedWord_onlyThatWordUnmatched() {
        assertThat(SpeechMatcher.match("Not now.", "now")).isEqualTo(
            listOf(MarkedWord("Not", false), MarkedWord("now", true))
        )
    }

    @Test
    fun match_halfOfAContractionHeard_contractionUnmatched() {
        assertThat(SpeechMatcher.match("I’m here.", "I here")).isEqualTo(
            listOf(MarkedWord("I’m", false), MarkedWord("here", true))
        )
    }

    @Test
    fun match_wordsInAnotherOrder_onlyTheLongestInOrderRunMatches() {
        assertThat(SpeechMatcher.match("Light, right.", "right light").count { it.matched }).isEqualTo(1)
    }

    @Test
    fun match_nothingHeard_noWordMatched() {
        assertThat(SpeechMatcher.match("Hello.", "")).isEqualTo(listOf(MarkedWord("Hello", false)))
    }
}
