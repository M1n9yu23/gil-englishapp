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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import assertk.assertions.isTrue
import com.gyugle.gil.core.designsystem.theme.GilTheme
import com.gyugle.gil.core.designsystem.theme.Spacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Inside both the fade and the slide, which last 150 and 250ms. */
private const val MID_TRANSITION_MILLIS = 80L
private const val FRAME_MILLIS = 16L

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en", application = Application::class)
class MainHostTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    private fun text(@StringRes id: Int) = composeRule.activity.getString(id)

    private fun tab(@StringRes label: Int) = composeRule.onNode(hasText(text(label)) and isSelectable())

    /**
     * With the clock held, steps frame by frame until the screen with [text] is composed, then to the
     * middle of the transition, and reads where its left edge is.
     */
    private fun leftEdgeMidTransition(text: String): Dp {
        var elapsed = 0L
        while (composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            check(elapsed < MID_TRANSITION_MILLIS) { "'$text' did not appear" }
            composeRule.mainClock.advanceTimeByFrame()
            elapsed += FRAME_MILLIS
        }
        composeRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS - elapsed)
        return composeRule.onNodeWithText(text).getUnclippedBoundsInRoot().left
    }

    private fun setMainHost() = composeRule.setContent { GilTheme { MainHost(entries = { testMainEntries(it) }) } }

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
        composeRule.onNodeWithText(TEST_PATH_ROOT).assertExists()
    }

    @Test
    fun mainHost_screenPushedInTab_hidesBarUntilBack() {
        setMainHost()
        tab(R.string.main_tab_path).performClick()

        composeRule.onNodeWithText(TEST_OPEN_PAGE).performClick()

        composeRule.onNodeWithText(TEST_PAGE).assertExists()
        tab(R.string.main_tab_path).assertDoesNotExist()

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText(TEST_PATH_ROOT).assertExists()
        tab(R.string.main_tab_path).assertIsSelected()
    }

    @Test
    fun mainHost_screenPushedInTab_slidesIn() {
        setMainHost()
        tab(R.string.main_tab_path).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.onNodeWithText(TEST_OPEN_PAGE).performClick()
        composeRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)

        assertThat(composeRule.onNodeWithText(TEST_PAGE).getUnclippedBoundsInRoot().left).isGreaterThan(0.dp)
    }

    @Test
    fun mainHost_backInTab_slidesTheRootInFromTheStart() {
        setMainHost()
        tab(R.string.main_tab_path).performClick()
        composeRule.onNodeWithText(TEST_OPEN_PAGE).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        assertThat(leftEdgeMidTransition(TEST_PATH_ROOT)).isLessThan(0.dp)
    }

    @Test
    fun mainHost_backAtAnotherTabRoot_fadesWithoutSliding() {
        setMainHost()
        tab(R.string.main_tab_words).performClick()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        assertThat(leftEdgeMidTransition(text(R.string.home_today_empty_title))).isEqualTo(Spacing().screenMargin)
    }

    @Test
    fun mainHost_tabSwitched_fadesWithoutSliding() {
        setMainHost()
        composeRule.waitForIdle()
        composeRule.mainClock.autoAdvance = false

        tab(R.string.main_tab_path).performClick()
        composeRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)

        assertThat(composeRule.onNodeWithText(TEST_PATH_ROOT).getUnclippedBoundsInRoot().left).isEqualTo(0.dp)
    }

    @Test
    fun mainHost_savedAndRestored_keepsSelectedTab() {
        val restorationTester = StateRestorationTester(composeRule)
        restorationTester.setContent { GilTheme { MainHost(entries = { testMainEntries(it) }) } }
        tab(R.string.main_tab_words).performClick()

        restorationTester.emulateSavedInstanceStateRestore()

        tab(R.string.main_tab_words).assertIsSelected()
        composeRule.onNodeWithText(text(R.string.dictionary_words_empty_title)).assertExists()
    }
}
