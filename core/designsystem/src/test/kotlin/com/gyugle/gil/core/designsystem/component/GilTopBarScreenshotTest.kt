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
package com.gyugle.gil.core.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.gyugle.gil.core.designsystem.captureOnPaper
import com.gyugle.gil.core.designsystem.icon.GilIcons
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "ko-w360dp-h640dp-xhdpi")
class GilTopBarScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun bars() = composeRule.captureOnPaper {
        Column {
            GilTopBar(title = "단어장")
            GilTopBar(
                title = "카페에서 주문하기와 계산하기",
                onNavigateBack = {},
                actions = {
                    IconButton(onClick = {}) { Icon(painter = GilIcons.Close, contentDescription = null) }
                }
            )
        }
    }

    @Test fun topBar_light_matchesDesign() = bars()

    @Test
    @Config(qualifiers = "+night")
    fun topBar_night_usesNightPaper() = bars()

    @Test
    @Config(fontScale = 2f)
    fun topBar_fontScale2_wrapsTitleToTwoLines() = bars()
}
