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
package com.gyugle.gil.core.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import org.junit.jupiter.api.Test

class NavigatorTest {

    private data object Today : NavKey

    private data object Path : NavKey

    private data object Words : NavKey

    private data class UnitDetail(val id: String) : NavKey

    private data class Lesson(val id: String) : NavKey

    private data object LessonResult : NavKey

    private val state = navigationState(Today, Path, Words)
    private val navigator = Navigator(state)

    private fun navigationState(start: NavKey, vararg others: NavKey): NavigationState {
        val routes = listOf(start) + others
        return NavigationState(start, mutableStateOf(start), routes.associateWith { NavBackStack(it) })
    }

    private fun stack(route: NavKey): List<NavKey> = state.backStacks.getValue(route).toList()

    @Test
    fun goTo_nonTopLevelRoute_pushesOnCurrentStack() {
        navigator.goTo(Lesson("a"))

        assertThat(stack(Today)).containsExactly(Today, Lesson("a"))
    }

    @Test
    fun goTo_sameRouteAsTop_doesNotPushTwice() {
        navigator.goTo(Lesson("a"))
        navigator.goTo(Lesson("a"))

        assertThat(stack(Today)).containsExactly(Today, Lesson("a"))
    }

    @Test
    fun goTo_sameRouteWithOtherArgument_pushes() {
        navigator.goTo(Lesson("a"))
        navigator.goTo(Lesson("b"))

        assertThat(stack(Today)).containsExactly(Today, Lesson("a"), Lesson("b"))
    }

    @Test
    fun goTo_topLevelRoute_switchesTabAndKeepsItsHistory() {
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))
        navigator.goTo(Today)
        navigator.goTo(Path)

        assertThat(state.topLevelRoute).isEqualTo(Path)
        assertThat(stack(Path)).containsExactly(Path, UnitDetail("u1"))
        assertThat(stack(Today)).containsExactly(Today)
    }

    @Test
    fun stacksInUse_onStartTab_onlyStartStack() {
        assertThat(state.stacksInUse).containsExactly(Today)
    }

    @Test
    fun stacksInUse_onOtherTab_startStackUnderneath() {
        navigator.goTo(Words)

        assertThat(state.stacksInUse).containsExactly(Today, Words)
    }

    @Test
    fun goBack_aboveRoot_popsCurrentStack() {
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))

        navigator.goBack()

        assertThat(stack(Path)).containsExactly(Path)
        assertThat(state.topLevelRoute).isEqualTo(Path)
    }

    @Test
    fun goBack_atRootOfOtherTab_returnsToStartTab() {
        navigator.goTo(Words)

        navigator.goBack()

        assertThat(state.topLevelRoute).isEqualTo(Today)
    }

    @Test
    fun goBack_atRootOfStartTab_keepsRoot() {
        navigator.goBack()

        assertThat(state.topLevelRoute).isEqualTo(Today)
        assertThat(stack(Today)).containsExactly(Today)
    }

    @Test
    fun goToAndPopUpTo_exclusive_keepsPopUpToRoute() {
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))
        navigator.goTo(Lesson("a"))

        navigator.goToAndPopUpTo(LessonResult, popUpTo = UnitDetail("u1"))

        assertThat(stack(Path)).containsExactly(Path, UnitDetail("u1"), LessonResult)
    }

    @Test
    fun goToAndPopUpTo_inclusive_removesPopUpToRoute() {
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))
        navigator.goTo(Lesson("a"))

        navigator.goToAndPopUpTo(LessonResult, popUpTo = Lesson("a"), inclusive = true)

        assertThat(stack(Path)).containsExactly(Path, UnitDetail("u1"), LessonResult)
    }

    @Test
    fun goToAndPopUpTo_inclusiveRoot_keepsRoot() {
        navigator.goTo(Lesson("a"))

        navigator.goToAndPopUpTo(LessonResult, popUpTo = Today, inclusive = true)

        assertThat(stack(Today)).containsExactly(Today, LessonResult)
    }

    @Test
    fun goToAndPopUpTo_routeNotInStack_onlyPushes() {
        navigator.goTo(Lesson("a"))

        navigator.goToAndPopUpTo(LessonResult, popUpTo = UnitDetail("missing"))

        assertThat(stack(Today)).containsExactly(Today, Lesson("a"), LessonResult)
    }

    @Test
    fun goToAndPopUpTo_onlyTouchesCurrentStack() {
        navigator.goTo(Lesson("a"))
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))

        navigator.goToAndPopUpTo(LessonResult, popUpTo = Path)

        assertThat(stack(Path)).containsExactly(Path, LessonResult)
        assertThat(stack(Today)).containsExactly(Today, Lesson("a"))
    }

    @Test
    fun goToOrBackTo_routeNotInStack_pushes() {
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))

        navigator.goToOrBackTo(UnitDetail("u2"))

        assertThat(stack(Path)).containsExactly(Path, UnitDetail("u1"), UnitDetail("u2"))
    }

    @Test
    fun goToOrBackTo_routeInStack_dropsWhatIsAboveIt() {
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))
        navigator.goTo(UnitDetail("u2"))
        navigator.goTo(Lesson("a"))

        navigator.goToOrBackTo(UnitDetail("u1"))

        assertThat(stack(Path)).containsExactly(Path, UnitDetail("u1"))
    }

    @Test
    fun goToOrBackTo_routeOnTop_changesNothing() {
        navigator.goTo(Lesson("a"))

        navigator.goToOrBackTo(Lesson("a"))

        assertThat(stack(Today)).containsExactly(Today, Lesson("a"))
    }

    @Test
    fun goToOrBackTo_tab_isRejected() {
        assertFailure { navigator.goToOrBackTo(Path) }.isInstanceOf(IllegalArgumentException::class)
    }

    @Test
    fun resetToStart_withHistory_everyTabBackToRoot() {
        navigator.goTo(Lesson("a"))
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("u1"))

        navigator.resetToStart()

        assertThat(state.topLevelRoute).isEqualTo(Today)
        assertThat(stack(Today)).containsExactly(Today)
        assertThat(stack(Path)).containsExactly(Path)
    }

    @Test
    fun open_knownTab_replacesTabHistoryAndShowsIt() {
        navigator.goTo(Path)
        navigator.goTo(UnitDetail("old"))
        navigator.goTo(Today)

        val opened = navigator.open(DeepLinkTarget(Path, listOf(UnitDetail("u2"), Lesson("b"))))

        assertThat(opened).isTrue()
        assertThat(state.topLevelRoute).isEqualTo(Path)
        assertThat(stack(Path)).containsExactly(Path, UnitDetail("u2"), Lesson("b"))
    }

    @Test
    fun open_unknownTab_changesNothing() {
        navigator.goTo(Lesson("a"))

        val opened = navigator.open(DeepLinkTarget(LessonResult))

        assertThat(opened).isFalse()
        assertThat(state.topLevelRoute).isEqualTo(Today)
        assertThat(stack(Today)).containsExactly(Today, Lesson("a"))
    }

    @Test
    fun singleStackHost_goBackAtRoot_staysAtRoot() {
        val single = navigationState(Today)
        val singleNavigator = Navigator(single)
        singleNavigator.goTo(Lesson("a"))

        singleNavigator.goBack()
        singleNavigator.goBack()

        assertThat(single.backStacks.getValue(Today).toList()).containsExactly(Today)
        assertThat(single.stacksInUse).containsExactly(Today)
    }

    @Test
    fun navigationState_startRouteWithoutStack_throws() {
        assertFailure {
            NavigationState(Today, mutableStateOf(Today), mapOf(Path to NavBackStack(Path)))
        }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun navigationState_stackNotStartingWithItsRoute_throws() {
        assertFailure {
            NavigationState(Today, mutableStateOf(Today), mapOf(Today to NavBackStack(Path)))
        }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun goTo_currentTab_changesNothing() {
        navigator.goTo(Lesson("a"))

        navigator.goTo(Today)

        assertThat(state.topLevelRoute).isEqualTo(Today)
        assertThat(stack(Today)).containsExactly(Today, Lesson("a"))
    }

    @Test
    fun goToAndPopUpTo_tabRoute_throws() {
        assertFailure { navigator.goToAndPopUpTo(Path, popUpTo = Today) }.isInstanceOf<IllegalArgumentException>()
    }

    @Test
    fun open_routesContainTab_changesNothing() {
        val opened = navigator.open(DeepLinkTarget(Path, listOf(Words)))

        assertThat(opened).isFalse()
        assertThat(state.topLevelRoute).isEqualTo(Today)
        assertThat(stack(Path)).containsExactly(Path)
    }
}
