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
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.gyugle.gil.core.navigation.NavigationState
import com.gyugle.gil.core.navigation.Navigator
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Serializable private data object FirstTab : NavKey

@Serializable private data object SecondTab : NavKey

/** A screen that any tab can push, such as a word page. */
@Serializable private data object SharedScreen : NavKey

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class NavigationEntriesTest {
    @get:Rule val composeRule = createComposeRule()

    private val viewModels = mutableListOf<SharedViewModel>()

    /** FirstTab and SecondTab each with SharedScreen on top; SecondTab showing. */
    private fun showSharedScreenOnBothTabs(): Navigator {
        val state = NavigationState(
            startRoute = FirstTab,
            topLevelRoute = mutableStateOf(SecondTab),
            backStacks = mapOf(
                FirstTab to NavBackStack(FirstTab, SharedScreen),
                SecondTab to NavBackStack(SecondTab, SharedScreen)
            )
        )
        val navigator = Navigator(state)
        composeRule.setContent {
            NavDisplay(
                entries = state.toEntries(
                    entryProvider {
                        entry<FirstTab> { Text("first") }
                        entry<SecondTab> { Text("second") }
                        entry<SharedScreen> {
                            val viewModel = viewModel<SharedViewModel>()
                            SideEffect { if (viewModel !in viewModels) viewModels += viewModel }
                            var taps by rememberSaveable { mutableIntStateOf(0) }
                            Text("taps $taps", Modifier.clickable { taps++ })
                        }
                    }
                ),
                onBack = navigator::goBack
            )
        }
        return navigator
    }

    @Test
    fun toEntries_sameKeyOnTopOfTwoTabs_keepsStatePerTab() {
        val navigator = showSharedScreenOnBothTabs()
        composeRule.onNodeWithText("taps 0").performClick()
        composeRule.onNodeWithText("taps 1").assertExists()

        composeRule.runOnIdle { navigator.goTo(FirstTab) }

        composeRule.onNodeWithText("taps 0").assertExists()
    }

    @Test
    fun toEntries_otherTabShownAndBack_keepsSavedState() {
        val navigator = showSharedScreenOnBothTabs()
        composeRule.onNodeWithText("taps 0").performClick()

        composeRule.runOnIdle { navigator.goTo(FirstTab) }
        composeRule.onNodeWithText("taps 0").assertExists()
        composeRule.runOnIdle { navigator.goTo(SecondTab) }

        composeRule.onNodeWithText("taps 1").assertExists()
    }

    @Test
    fun toEntries_sameKeyOnTopOfTwoTabs_getsOneViewModelPerTab() {
        val navigator = showSharedScreenOnBothTabs()

        composeRule.runOnIdle { navigator.goTo(FirstTab) }
        composeRule.runOnIdle { navigator.goTo(SecondTab) }

        composeRule.runOnIdle {
            assertThat(viewModels).hasSize(2)
            assertThat(viewModels.none { it.cleared }).isTrue()
        }
    }

    @Test
    fun toEntries_screenPoppedFromOneTab_clearsOnlyThatTabsViewModel() {
        val navigator = showSharedScreenOnBothTabs()
        composeRule.runOnIdle { navigator.goTo(FirstTab) }
        composeRule.runOnIdle { navigator.goTo(SecondTab) }

        composeRule.runOnIdle { navigator.goBack() }

        composeRule.onNodeWithText("second").assertExists()
        composeRule.runOnIdle { assertThat(viewModels.count { it.cleared }).isEqualTo(1) }
    }
}

class SharedViewModel : ViewModel() {
    var cleared = false
        private set

    override fun onCleared() {
        cleared = true
    }
}
