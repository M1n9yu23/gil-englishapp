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
package com.gyugle.gil.feature.curriculum.impl.presentation.unitdetail

import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gyugle.gil.feature.curriculum.api.ContentError
import com.gyugle.gil.feature.curriculum.impl.presentation.captureScreen
import com.gyugle.gil.feature.curriculum.impl.presentation.toContentErrorUi
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h760dp-xhdpi")
class UnitDetailScreenScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun capture(state: UnitDetailState) = composeRule.captureScreen {
        UnitDetailScreen(state = state, onAction = {}, onBack = {})
    }

    private val loaded = UnitDetailState(isLoading = false, unit = UnitDetailPreviewData.unit)

    @Test fun lessons_light_showsNumberedLessons() = capture(loaded)

    @Test
    @Config(qualifiers = "+night")
    fun lessons_night_usesNightPaper() = capture(loaded)

    @Test
    @Config(qualifiers = "en-w360dp-h760dp-xhdpi", fontScale = 2f)
    fun lessons_englishFontScale2_wrapsTitleAndGoals() = capture(loaded)

    @Test fun loading_light_showsPlaceholders() = capture(UnitDetailState())

    @Test
    fun notFound_light_offersTheWayBack() =
        capture(UnitDetailState(isLoading = false, error = ContentError.NOT_FOUND.toContentErrorUi()))
}
