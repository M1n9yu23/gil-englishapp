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
package com.gyugle.gil.feature.onboarding.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** The steps of the first run, in order. Each one is its own entry, so back returns a step. */
@Serializable
sealed interface OnboardingRoute : NavKey

/** The start of the first run, with the way to skip the rest. */
@Serializable
@SerialName("onboarding.welcome")
data object OnboardingWelcomeRoute : OnboardingRoute

/** One of the pages on how to study with the app, [page] from 0 to [PAGE_COUNT] - 1. */
@Serializable
@SerialName("onboarding.method")
data class OnboardingMethodRoute(val page: Int) : OnboardingRoute {
    init {
        require(page in 0 until PAGE_COUNT) { "No method page $page" }
    }

    companion object {
        const val PAGE_COUNT = 4
    }
}

@Serializable
@SerialName("onboarding.goal")
data object OnboardingGoalRoute : OnboardingRoute

@Serializable
@SerialName("onboarding.reminder")
data object OnboardingReminderRoute : OnboardingRoute

/** The last step: plays a word to check that the phone can speak English. */
@Serializable
@SerialName("onboarding.sound")
data object OnboardingSoundRoute : OnboardingRoute
