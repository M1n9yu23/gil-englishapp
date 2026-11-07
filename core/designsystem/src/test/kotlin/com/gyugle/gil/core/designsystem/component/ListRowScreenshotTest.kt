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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
class ListRowScreenshotTest {
    @get:Rule val composeRule = createComposeRule()

    private fun rows() = composeRule.captureOnPaper {
        Column {
            ListRow(
                headline = "인사와 소개",
                supporting = "단어 12개, 문장 8개",
                onClick = {},
                trailing = { Icon(painter = GilIcons.ChevronRight, contentDescription = null) }
            )
            HorizontalDivider()
            ListRow(
                headline = "카페에서 주문하기",
                supporting = "다 배웠어요",
                onClick = {},
                leading = { Icon(painter = GilIcons.Check, contentDescription = null) },
                trailing = { Icon(painter = GilIcons.ChevronRight, contentDescription = null) }
            )
            HorizontalDivider()
            ListRow(headline = "알림 시간", supporting = "오후 8:00")
        }
    }

    @Test fun listRow_light_matchesDesign() = rows()

    @Test
    @Config(qualifiers = "+night")
    fun listRow_night_usesNightPaper() = rows()

    @Test
    @Config(fontScale = 2f)
    fun listRow_fontScale2_wrapsText() = rows()
}
