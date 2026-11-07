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
package com.gyugle.gil

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import assertk.assertThat
import assertk.assertions.isTrue
import com.gyugle.gil.core.designsystem.theme.GilTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en", application = Application::class)
class MainHostTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int) = composeRule.activity.getString(id)

    private fun tab(@StringRes label: Int) = composeRule.onNode(hasText(text(label)) and isSelectable())

    private fun setMainHost() = composeRule.setContent { GilTheme { MainHost() } }

    @Test
    fun mainHost_started_showsTodaySelected() {
        setMainHost()

        tab(R.string.main_tab_today).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.home_today_empty_title)).assertExists()
    }

    @Test
    fun mainHost_tabClicked_showsThatTab() {
        setMainHost()

        tab(R.string.main_tab_words).performClick()

        tab(R.string.main_tab_words).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.dictionary_words_empty_title)).assertExists()
    }

    @Test
    fun mainHost_backAtAnotherTabRoot_returnsToToday() {
        setMainHost()
        tab(R.string.main_tab_progress).performClick()
        composeRule.waitForIdle()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        tab(R.string.main_tab_today).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.home_today_empty_title)).assertExists()
    }

    @Test
    fun mainHost_backAtTodayRoot_leavesTheApp() {
        setMainHost()
        composeRule.waitForIdle()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.runOnIdle { assertThat(composeRule.activity.isFinishing).isTrue() }
    }

    @Test
    fun mainHost_todayEmptyActionClicked_opensPath() {
        setMainHost()

        composeRule.onNodeWithText(text(R.string.home_today_empty_action)).performClick()

        tab(R.string.main_tab_path).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.curriculum_path_empty_title)).assertExists()
    }

    @Test
    fun mainHost_savedAndRestored_keepsSelectedTab() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { GilTheme { MainHost() } }
        tab(R.string.main_tab_words).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        tab(R.string.main_tab_words).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.dictionary_words_empty_title)).assertExists()
    }
}
