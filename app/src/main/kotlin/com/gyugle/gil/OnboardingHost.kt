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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.core.navigation.rememberNavigationState
import com.gyugle.gil.feature.onboarding.api.OnboardingWelcomeRoute
import com.gyugle.gil.feature.onboarding.impl.presentation.onboardingEntries

/**
 * The first run: one stack from the welcome screen. Back from the welcome screen leaves the app.
 * The activity swaps this for [MainHost] once onboarding is complete.
 */
@Composable
fun OnboardingHost(
    modifier: Modifier = Modifier,
    entries: EntryProviderScope<NavKey>.(Navigator) -> Unit = { navigator -> onboardingEntries(navigator) }
) {
    val navigationState = rememberNavigationState(OnboardingWelcomeRoute)
    val navigator = remember(navigationState) { Navigator(navigationState) }
    val transitions = rememberStackTransitions()
    // The steps pad themselves for the system bars, like a pushed screen in the main host.
    Surface(modifier = modifier.fillMaxSize()) {
        NavDisplay(
            entries = navigationState.toEntries(entryProvider { entries(navigator) }),
            transitionSpec = { transitions.push },
            popTransitionSpec = { transitions.pop },
            predictivePopTransitionSpec = { _ -> transitions.pop },
            onBack = navigator::goBack
        )
    }
}
