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
package com.gyugle.gil.feature.onboarding.impl.presentation.method

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.gyugle.gil.feature.onboarding.api.OnboardingMethodRoute
import org.junit.jupiter.api.Test

class MethodPageTest {
    // A route for a page without content would crash; a page without a route would never show.
    @Test
    fun entries_matchTheRoutesPageCount() {
        assertThat(MethodPage.entries.size).isEqualTo(OnboardingMethodRoute.PAGE_COUNT)
    }
}
