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
package com.gyugle.gil.feature.onboarding.impl.presentation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.gyugle.gil.core.navigation.Navigator
import com.gyugle.gil.feature.onboarding.api.OnboardingGoalRoute
import com.gyugle.gil.feature.onboarding.api.OnboardingMethodRoute
import com.gyugle.gil.feature.onboarding.api.OnboardingReminderRoute
import com.gyugle.gil.feature.onboarding.api.OnboardingSoundRoute
import com.gyugle.gil.feature.onboarding.api.OnboardingWelcomeRoute
import com.gyugle.gil.feature.onboarding.api.StudyMethodRoute
import com.gyugle.gil.feature.onboarding.impl.presentation.goal.GoalStepRoot
import com.gyugle.gil.feature.onboarding.impl.presentation.method.MethodScreen
import com.gyugle.gil.feature.onboarding.impl.presentation.method.StudyMethodScreen
import com.gyugle.gil.feature.onboarding.impl.presentation.reminder.ReminderStepRoot
import com.gyugle.gil.feature.onboarding.impl.presentation.sound.SoundCheckRoot
import com.gyugle.gil.feature.onboarding.impl.presentation.welcome.WelcomeRoot

/**
 * The first run, one entry per step. It ends when skipping or the sound check marks onboarding
 * complete; the app watches that and switches to the main screens.
 */
fun EntryProviderScope<NavKey>.onboardingEntries(navigator: Navigator) {
    entry<OnboardingWelcomeRoute> {
        WelcomeRoot(onStart = { navigator.goTo(OnboardingMethodRoute(page = 0)) })
    }
    entry<OnboardingMethodRoute> { route ->
        MethodScreen(
            page = route.page,
            onNext = {
                val next = route.page + 1
                navigator.goTo(
                    if (next < OnboardingMethodRoute.PAGE_COUNT) OnboardingMethodRoute(next) else OnboardingGoalRoute
                )
            },
            onBack = navigator::goBack
        )
    }
    entry<OnboardingGoalRoute> {
        GoalStepRoot(onNext = { navigator.goTo(OnboardingReminderRoute) }, onBack = navigator::goBack)
    }
    entry<OnboardingReminderRoute> {
        ReminderStepRoot(onNext = { navigator.goTo(OnboardingSoundRoute) }, onBack = navigator::goBack)
    }
    entry<OnboardingSoundRoute> {
        SoundCheckRoot(onBack = navigator::goBack)
    }
}

/** The method pages for the main screens, opened again after the first run. */
fun EntryProviderScope<NavKey>.studyMethodEntries(navigator: Navigator) {
    entry<StudyMethodRoute> {
        StudyMethodScreen(onClose = navigator::goBack)
    }
}
