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

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.dictionary.impl.presentation.captureScreen
import com.gyugle.gil.feature.dictionary.impl.presentation.toContentErrorUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class WordsScreenScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: WordsState) = composeRule.captureScreen {
        WordsScreen(state = state, onAction = {}, onOpenEntry = {})
    }

    private val saved = WordsState(saved = WordListUi.Loaded(WordsPreviewData.saved))
    private val search = WordsState(tab = WordsTab.SEARCH, query = "thank", search = WordsPreviewData.search)
    private val mistakes = WordsState(tab = WordsTab.MISTAKES, mistakes = WordListUi.Loaded(WordsPreviewData.mistakes))

    @Test fun saved_light_listsWordsPhrasesAndSounds() = capture(saved)

    @Test
    @Config(qualifiers = "+night")
    fun saved_night_usesNightPaper() = capture(saved)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun saved_englishFontScale2_keepsTabsAndRowsWhole() = capture(saved)

    @Test fun savedEmpty_light_explainsHowToSave() = capture(WordsState(saved = WordListUi.Loaded(emptyList())))

    @Test fun loading_light_showsPlaceholders() = capture(WordsState())

    @Test
    fun failed_light_offersRetry() =
        capture(WordsState(saved = WordListUi.Failed(ContentError.UNAVAILABLE.toContentErrorUi())))

    @Test fun search_light_putsLearnedWordsFirst() = capture(search)

    @Test
    @Config(qualifiers = "+night")
    fun search_night_putsLearnedWordsFirst() = capture(search)

    @Test
    @Config(fontScale = 2f)
    fun search_fontScale2_wrapsStageNotes() = capture(search)

    @Test fun searchIdle_light_showsTheHint() = capture(WordsState(tab = WordsTab.SEARCH))

    @Test
    fun searchNoResults_light_suggestsCheckingTheSpelling() =
        capture(WordsState(tab = WordsTab.SEARCH, query = "tabel", search = SearchUi.NoResults("tabel")))

    @Test fun mistakes_light_showsCounts() = capture(mistakes)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi")
    fun mistakes_english_showsCounts() = capture(mistakes)

    @Test
    fun mistakesEmpty_light_saysWhereMistakesComeFrom() =
        capture(WordsState(tab = WordsTab.MISTAKES, mistakes = WordListUi.Loaded(emptyList())))
}
